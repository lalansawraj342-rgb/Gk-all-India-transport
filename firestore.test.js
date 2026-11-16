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
    vehicleType: "10 Wheeler",
    goodsDescription: "Industrial machinery crates",
    goodsWeight: "14 Tons",
    specialInstructions: "Need tarpaulin cover",
    status: "New Booking",
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

test("Unauthenticated user cannot read or write bookings, users, or vehicles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("bookings").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("vehicles").get());
});

test("Customer can create and read own user profile, cannot read other user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Sharma",
      mobileNumber: "9876543210",
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

test("Customer can create booking ONLY with New Booking status (auto-confirm blocked)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("bookings").doc("b1").set(validBookingPayload(ALICE_UID))
  );

  // Customer trying to create a pre-confirmed booking MUST FAIL
  await assertFails(
    aliceDb.collection("bookings").doc("b2").set(
      validBookingPayload(ALICE_UID, { status: "Confirmed" })
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

  // Filtered by own userId succeeds
  await assertSucceeds(
    aliceDb.collection("bookings").where("userId", "==", ALICE_UID).get()
  );

  // Unfiltered list by non-admin fails (rules are not filters)
  await assertFails(aliceDb.collection("bookings").get());

  // Cross-user get fails
  await assertFails(bobDb.collection("bookings").doc("b_alice").get());
});

test("Customer cannot self-confirm booking, can cancel New Booking only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("bookings").doc("b_status").set(validBookingPayload(ALICE_UID))
  );

  // Self-confirming must fail
  await assertFails(
    aliceDb.collection("bookings").doc("b_status").update({
      status: "Confirmed",
      updatedAt: pastTimestamp(),
    })
  );

  // Customer cancelling New Booking succeeds
  await assertSucceeds(
    aliceDb.collection("bookings").doc("b_status").update({
      status: "Cancelled",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofAdminDb.collection("bookings").get());
  await assertFails(spoofAdminDb.collection("vehicles").get());
});

test("Verified Admin can list all bookings, confirm, assign vehicle, and manage fleet", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("bookings").doc("b_admin_test").set(validBookingPayload(ALICE_UID))
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  // Admin can list all bookings
  await assertSucceeds(adminDb.collection("bookings").get());

  // Admin can add fleet vehicle
  const now = pastTimestamp();
  await assertSucceeds(
    adminDb.collection("vehicles").doc("v1").set({
      vehicleNumber: "MH-12-TR-4590",
      vehicleType: "10 Wheeler",
      driverName: "Suresh Yadav",
      driverPhone: "9811122233",
      availability: "Available",
      currentCity: "Mumbai",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin can confirm and assign vehicle
  await assertSucceeds(
    adminDb.collection("bookings").doc("b_admin_test").update({
      status: "Vehicle Assigned",
      assignedVehicleId: "v1",
      assignedVehicleNumber: "MH-12-TR-4590",
      assignedDriverName: "Suresh Yadav",
      assignedDriverPhone: "9811122233",
      estimatedFare: "₹38,500",
      updatedAt: pastTimestamp(),
    })
  );
});
