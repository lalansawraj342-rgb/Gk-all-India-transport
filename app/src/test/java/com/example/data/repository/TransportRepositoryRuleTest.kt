package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.VehicleType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createBooking_forcesPendingStatus_andAppearsInCustomerStream() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TransportRepository(firestore, auth)

    // Even if client passes ACCEPTED, createBooking must force PENDING & UNPAID and succeed
    val booking =
      Booking(
        userId = uid,
        customerName = "Ramesh Verma",
        customerPhone = "9876543210",
        pickupLocation = "Delhi, Okhla Phase 2",
        deliveryLocation = "Jaipur, Vishwakarma Industrial Area",
        bookingDateTimeMillis = 1795000000000L,
        bookingDateLabel = "18 Nov 2026",
        bookingTimeLabel = "09:00 AM",
        vehicleType = VehicleType.TRUCK_407.label,
        goodsDescription = "Steel pipes & fittings",
        goodsWeight = "2.5 Tons",
        goodsPhotos = listOf("data:image/jpeg;base64,/9j/4AAQSkZJRg=="),
        specialInstructions = "Call before arrival",
        status = BookingStatus.ACCEPTED.label,
      )

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.createBooking(booking) }
    assertTrue("Expected booking creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
    val (bookingId, bookingCode) = createResult.getOrThrow()
    assertTrue(bookingId.isNotBlank())
    assertTrue(bookingCode.startsWith("GK-"))

    val observed =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCustomerBookings(uid).first { list -> list.any { it.id == bookingId } }
      }
    val created = observed.first { it.id == bookingId }
    assertEquals(BookingStatus.PENDING.label, created.status)
    assertEquals(PaymentStatus.UNPAID.label, created.paymentStatus)
    assertEquals("Ramesh Verma", created.customerName)
    assertEquals(1, created.goodsPhotos.size)
  }

  @Test
  fun saveUserProfile_withAddressAndAltPhone_roundTripsCleanly() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TransportRepository(firestore, auth)

    val saveResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveUserProfile(
          name = "Ramesh Verma",
          mobileNumber = "9876543210",
          email = ALICE_EMAIL,
          city = "Delhi",
          address = "Plot 12, Transport Nagar, Delhi",
          alternatePhone = "9142631727",
          preferredLanguage = "hi",
        )
      }
    assertTrue("Expected profile save to succeed", saveResult.isSuccess)

    val profile =
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile(uid).first { it != null } }
    assertNotNull(profile)
    assertEquals("Ramesh Verma", profile?.name)
    assertEquals("9876543210", profile?.mobileNumber)
    assertEquals("Plot 12, Transport Nagar, Delhi", profile?.address)
    assertEquals("9142631727", profile?.alternatePhone)
    assertEquals("hi", profile?.preferredLanguage)
  }

  @Test
  fun submitCustomerPayment_createsPendingVerification_andAppearsInStream() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TransportRepository(firestore, auth)
    val (bookingId, bookingCode) =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .createBooking(
            Booking(
              userId = uid,
              customerName = "Ramesh Verma",
              customerPhone = "9876543210",
              pickupLocation = "Delhi",
              deliveryLocation = "Patna",
              bookingDateTimeMillis = 1795000000000L,
              bookingDateLabel = "20 Nov 2026",
              bookingTimeLabel = "10:00 AM",
              vehicleType = VehicleType.CONTAINER_TRUCK.label,
              goodsDescription = "Electronics",
              goodsWeight = "8 Tons",
            )
          )
          .getOrThrow()
      }

    val payResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.submitCustomerPayment(
          booking = Booking(id = bookingId, bookingCode = bookingCode, customerName = "Ramesh Verma"),
          amount = 15000.0,
          method = PaymentMethod.UPI.label,
          referenceId = "UTR99887766",
          notes = "Advance via UPI",
        )
      }
    assertTrue("Expected payment submission to succeed: ${payResult.exceptionOrNull()}", payResult.isSuccess)
    val payId = payResult.getOrThrow()

    val payments =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCustomerPayments(uid).first { list -> list.any { it.id == payId } }
      }
    val saved = payments.first { it.id == payId }
    assertEquals(PaymentRecordStatus.PENDING_VERIFICATION.label, saved.status)
    assertEquals(15000.0, saved.amount, 0.01)
  }

  @Test
  fun customerBooking_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = TransportRepository(firestore, auth)
    val (bookingId, _) =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .createBooking(
            Booking(
              userId = aliceUid,
              customerName = "Alice Customer",
              customerPhone = "9811111111",
              pickupLocation = "Mumbai",
              deliveryLocation = "Pune",
              bookingDateTimeMillis = 1795000000000L,
              bookingDateLabel = "19 Nov 2026",
              bookingTimeLabel = "11:00 AM",
              vehicleType = VehicleType.TATA_ACE.label,
              goodsDescription = "Electronics boxes",
              goodsWeight = "600 Kg",
            )
          )
          .getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = TransportRepository(firestore, auth)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getBookingById(bookingId) }
    assertTrue("Cross-user booking read must fail", result.isFailure)
    val firestoreErr = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreErr?.code)
  }

  @Test
  fun customerBooking_cancelOwnPendingBooking_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TransportRepository(firestore, auth)
    val (bookingId, _) =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .createBooking(
            Booking(
              userId = uid,
              customerName = "Sita Devi",
              customerPhone = "9822222222",
              pickupLocation = "Lucknow",
              deliveryLocation = "Kanpur",
              bookingDateTimeMillis = 1795000000000L,
              bookingDateLabel = "20 Nov 2026",
              bookingTimeLabel = "02:00 PM",
              vehicleType = VehicleType.PICKUP_TRUCK.label,
              goodsDescription = "Textile bales",
              goodsWeight = "1.5 Tons",
            )
          )
          .getOrThrow()
      }

    val cancelResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.cancelCustomerBooking(bookingId) }
    assertTrue("Customer cancelling own Pending booking should succeed", cancelResult.isSuccess)
  }

  @Test
  fun observeCustomerBookings_unauthenticated_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = TransportRepository(firestore, auth)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeCustomerBookings("some_uid").first() }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected unauthenticated query to throw", caught)
    val firestoreErr = caught?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreErr?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_transport@test.com"
    const val BOB_EMAIL = "bob_transport@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
