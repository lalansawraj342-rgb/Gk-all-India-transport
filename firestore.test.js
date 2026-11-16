const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_cust_1";
const BOB_UID = "bob_cust_2";
const ADMIN_UID = "admin_gk_1";
const ADMIN_EMAIL = "lraj65576@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validBookingPayload(userId, overrides = {}) {
  const now = pastTimestamp();
  return {
    userId,
    bookingCode: "GK-2026-1001",
    customerName: "Rajesh Kumar",
    customerPhone: "9876543210",
    pickupLocation: "Delhi, Azadpur Mandi",
    deliveryLocation: "Mumbai, Bhiwandi Hub",
    bookingDateTimeMillis: 1795000000000,
    bookingDateLabel: "18 Nov 2026",
    bookingTimeLabel: "10:30 AM",
    vehicleType: "14 Wheeler Truck",
    goodsDescription: "Industrial machinery crates",
    goodsWeight: "25 Tons",
    goodsPhotos: [],
    specialInstructions: "Need tarpaulin cover",
    status: "Pending",
    paymentStatus: "Unpaid",
    paymentMethod: "None",
    totalFareAmount: 0,
    paidAmount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write bookings, users, vehicles, drivers, payments, or notifications", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("bookings").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("vehicles").get());
  await assertFails(unauthDb.collection("drivers").get());
  await assertFails(unauthDb.collection("payments").get());
  await assertFails(unauthDb.collection("notifications").get());
});

test("Customer can create and read own user profile with address, cannot read other user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Sharma",
      mobileNumber: "9876543210",
      city: "Delhi",
      address: "Plot 14, Okhla Industrial Area Phase 2",
      alternatePhone: "9142631727",
      preferredLanguage: "hi",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Customer can create booking ONLY with Pending status and Unpaid paymentStatus", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("bookings").doc("b1").set(validBookingPayload(ALICE_UID))
  );

  // Customer trying to create a pre-accepted booking MUST FAIL
  await assertFails(
    aliceDb.collection("bookings").doc("b2").set(
      validBookingPayload(ALICE_UID, { status: "Accepted" })
    )
  );

  // Customer trying to mark booking Paid on create MUST FAIL
  await assertFails(
    aliceDb.collection("bookings").doc("b2b").set(
      validBookingPayload(ALICE_UID, { paymentStatus: "Paid" })
    )
  );

  // Customer trying to spoof another userId MUST FAIL
  await assertFails(
    aliceDb.collection("bookings").doc("b3").set(
      validBookingPayload(BOB_UID)
    )
  );

  // Shadow field injection MUST FAIL
  await assertFails(
    aliceDb.collection("bookings").doc("b4").set(
      validBookingPayload(ALICE_UID, { isVerified: true })
    )
  );
});

test("Customer query isolation: own filtered query succeeds, unfiltered or cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("bookings").doc("b_alice").set(validBookingPayload(ALICE_UID))
  );

  await assertSucceeds(
    aliceDb.collection("bookings").where("userId", "==", ALICE_UID).get()
  );

  await assertFails(aliceDb.collection("bookings").get());
  await assertFails(bobDb.collection("bookings").doc("b_alice").get());
});

test("Customer cannot self-accept booking, can cancel Pending booking only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("bookings").doc("b_status").set(validBookingPayload(ALICE_UID))
  );

  await assertFails(
    aliceDb.collection("bookings").doc("b_status").update({
      status: "Accepted",
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("bookings").doc("b_status").update({
      status: "Cancelled",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Customer can submit payment request in Pending Verification and read own payments, cannot auto-mark Paid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_1").set({
      bookingId: "b1",
      bookingCode: "GK-1001",
      userId: ALICE_UID,
      customerName: "Alice Sharma",
      amount: 5000,
      method: "UPI",
      status: "Pending Verification",
      referenceId: "UTR12345678",
      notes: "Advance payment",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("payments").doc("pay_2").set({
      bookingId: "b1",
      bookingCode: "GK-1001",
      userId: ALICE_UID,
      customerName: "Alice Sharma",
      amount: 5000,
      method: "UPI",
      status: "Paid",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("payments").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("payments").doc("pay_1").get());
});

test("Customer can send BOOKING_REQUEST notification to ADMIN and read own notifications", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("notifications").doc("n1").set({
      recipientUserId: "ADMIN",
      senderUserId: ALICE_UID,
      bookingId: "b1",
      bookingCode: "GK-1001",
      type: "BOOKING_REQUEST",
      title: "New Booking Request",
      message: "Alice requested a 14 Wheeler Truck",
      isRead: false,
      createdAt: now,
    })
  );

  // Customer cannot send fake BOOKING_ACCEPTED to another user
  await assertFails(
    aliceDb.collection("notifications").doc("n2").set({
      recipientUserId: BOB_UID,
      senderUserId: ALICE_UID,
      type: "BOOKING_ACCEPTED",
      title: "Fake",
      message: "Fake",
      isRead: false,
      createdAt: now,
    })
  );

  await assertFails(bobDb.collection("notifications").doc("n1").get());
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofAdminDb.collection("bookings").get());
  await assertFails(spoofAdminDb.collection("vehicles").get());
  await assertFails(spoofAdminDb.collection("drivers").get());
  await assertFails(spoofAdminDb.collection("users").get());
});

test("Verified Admin can manage bookings, drivers, vehicles, payments, customers, and notifications", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("bookings").doc("b_admin_test").set(validBookingPayload(ALICE_UID))
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(adminDb.collection("bookings").get());
  await assertSucceeds(adminDb.collection("users").get());

  const now = pastTimestamp();
  await assertSucceeds(
    adminDb.collection("vehicles").doc("v1").set({
      vehicleNumber: "MH-12-TR-4590",
      vehicleType: "14 Wheeler Truck",
      driverName: "Suresh Yadav",
      driverPhone: "9811122233",
      availability: "Available",
      currentCity: "Mumbai",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("drivers").doc("d1").set({
      name: "Suresh Yadav",
      mobileNumber: "9811122233",
      licenseNumber: "MH12-20190045678",
      assignedVehicleId: "v1",
      assignedVehicleNumber: "MH-12-TR-4590",
      availability: "Available",
      tripStatus: "Idle",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("bookings").doc("b_admin_test").update({
      status: "In Transit",
      assignedVehicleId: "v1",
      assignedVehicleNumber: "MH-12-TR-4590",
      assignedDriverId: "d1",
      assignedDriverName: "Suresh Yadav",
      assignedDriverPhone: "9811122233",
      estimatedFare: "₹38,500",
      totalFareAmount: 38500,
      paidAmount: 10000,
      paymentStatus: "Advance Paid",
      paymentMethod: "UPI",
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("notifications").doc("n_cust").set({
      recipientUserId: ALICE_UID,
      senderUserId: ADMIN_UID,
      bookingId: "b_admin_test",
      bookingCode: "GK-2026-1001",
      type: "VEHICLE_ASSIGNED",
      title: "Vehicle Assigned",
      message: "MH-12-TR-4590 assigned to your booking",
      isRead: false,
      createdAt: pastTimestamp(),
    })
  );
});
