package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminUser
import com.example.data.model.AppNotification
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.Driver
import com.example.data.model.FleetVehicle
import com.example.data.model.NotificationType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TransportRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val docRef = db.collection(COLLECTION_USERS).document(userId)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              UserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.GET, docRef.path, userId)
        }
    )
  }

  fun observeAllCustomers(): Flow<List<UserProfile>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(COLLECTION_USERS)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_USERS, uid)
        }
    )
  }

  suspend fun saveUserProfile(
    name: String,
    mobileNumber: String,
    email: String,
    city: String,
    address: String,
    alternatePhone: String,
    preferredLanguage: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection(COLLECTION_USERS).document(uid)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        val updates = mapOf(
          "name" to name.trim(),
          "mobileNumber" to mobileNumber.trim(),
          "email" to email.trim(),
          "city" to city.trim(),
          "address" to address.trim(),
          "alternatePhone" to alternatePhone.trim(),
          "preferredLanguage" to preferredLanguage,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updates).await()
      } else {
        val payload = mapOf(
          "userId" to uid,
          "name" to name.trim(),
          "mobileNumber" to mobileNumber.trim(),
          "email" to email.trim(),
          "city" to city.trim(),
          "address" to address.trim(),
          "alternatePhone" to alternatePhone.trim(),
          "preferredLanguage" to preferredLanguage,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun isUserAdmin(userId: String, email: String?, isEmailVerified: Boolean): Boolean {
    if (email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && isEmailVerified) {
      return true
    }
    val docRef = db.collection(COLLECTION_ADMINS).document(userId)
    return try {
      docRef.get().await().exists()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      false
    }
  }

  fun observeCustomerBookings(userId: String): Flow<List<Booking>> = flow {
    val path = COLLECTION_BOOKINGS
    emitAll(
      db.collection(COLLECTION_BOOKINGS)
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, path, userId)
        }
    )
  }

  suspend fun getBookingById(bookingId: String): Result<Booking?> {
    val docRef = db.collection(COLLECTION_BOOKINGS).document(bookingId)
    return try {
      val snap = docRef.get().await()
      val booking =
        snap.toObject(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      Result.success(booking)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createBooking(booking: Booking): Result<Pair<String, String>> {
    val uid = requireUserId()
    val docRef = db.collection(COLLECTION_BOOKINGS).document()
    val bookingCode = "GK-${(100000..999999).random()}"
    val payload =
      mapOf(
        "userId" to uid,
        "bookingCode" to bookingCode,
        "customerName" to booking.customerName.trim(),
        "customerPhone" to booking.customerPhone.trim(),
        "pickupLocation" to booking.pickupLocation.trim(),
        "deliveryLocation" to booking.deliveryLocation.trim(),
        "bookingDateTimeMillis" to booking.bookingDateTimeMillis,
        "bookingDateLabel" to booking.bookingDateLabel.trim(),
        "bookingTimeLabel" to booking.bookingTimeLabel.trim(),
        "vehicleType" to booking.vehicleType,
        "goodsDescription" to booking.goodsDescription.trim(),
        "goodsWeight" to booking.goodsWeight.trim(),
        "goodsPhotos" to booking.goodsPhotos.take(3),
        "specialInstructions" to booking.specialInstructions.trim(),
        // CRITICAL: Customers can NEVER receive an auto-accepted booking; always Pending.
        "status" to BookingStatus.PENDING.label,
        "assignedVehicleId" to "",
        "assignedVehicleNumber" to "",
        "assignedDriverId" to "",
        "assignedDriverName" to "",
        "assignedDriverPhone" to "",
        "adminNotes" to "",
        "estimatedFare" to "",
        "totalFareAmount" to 0.0,
        "paidAmount" to 0.0,
        "paymentStatus" to PaymentStatus.UNPAID.label,
        "paymentMethod" to PaymentMethod.NONE.label,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      // Send admin notification record (best-effort)
      sendNotificationInternal(
        recipientUserId = ADMIN_RECIPIENT,
        senderUserId = uid,
        bookingId = docRef.id,
        bookingCode = bookingCode,
        type = NotificationType.BOOKING_REQUEST.code,
        title = "New Booking Request ($bookingCode)",
        message =
          "${booking.customerName.trim()} requested ${booking.vehicleType}: ${booking.pickupLocation.trim()} → ${booking.deliveryLocation.trim()}",
      )
      Result.success(docRef.id to bookingCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun cancelCustomerBooking(bookingId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(COLLECTION_BOOKINGS).document(bookingId)
    return try {
      docRef
        .update(
          mapOf(
            "status" to BookingStatus.CANCELLED.label,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeAllBookings(): Flow<List<Booking>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(COLLECTION_BOOKINGS)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_BOOKINGS, uid)
        }
    )
  }

  suspend fun updateBookingByAdmin(
    bookingId: String,
    status: String,
    assignedVehicleId: String? = null,
    assignedVehicleNumber: String? = null,
    assignedDriverId: String? = null,
    assignedDriverName: String? = null,
    assignedDriverPhone: String? = null,
    adminNotes: String? = null,
    estimatedFare: String? = null,
    totalFareAmount: Double? = null,
    paidAmount: Double? = null,
    paymentStatus: String? = null,
    paymentMethod: String? = null,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection(COLLECTION_BOOKINGS).document(bookingId)
    val updates = mutableMapOf<String, Any>(
      "status" to status,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (assignedVehicleId != null) updates["assignedVehicleId"] = assignedVehicleId.trim()
    if (assignedVehicleNumber != null) updates["assignedVehicleNumber"] = assignedVehicleNumber.trim()
    if (assignedDriverId != null) updates["assignedDriverId"] = assignedDriverId.trim()
    if (assignedDriverName != null) updates["assignedDriverName"] = assignedDriverName.trim()
    if (assignedDriverPhone != null) updates["assignedDriverPhone"] = assignedDriverPhone.trim()
    if (adminNotes != null) updates["adminNotes"] = adminNotes.trim()
    if (estimatedFare != null) updates["estimatedFare"] = estimatedFare.trim()
    if (totalFareAmount != null) updates["totalFareAmount"] = totalFareAmount
    if (paidAmount != null) updates["paidAmount"] = paidAmount
    if (paymentStatus != null) updates["paymentStatus"] = paymentStatus
    if (paymentMethod != null) updates["paymentMethod"] = paymentMethod

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeFleetVehicles(): Flow<List<FleetVehicle>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(COLLECTION_VEHICLES)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(FleetVehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_VEHICLES, uid)
        }
    )
  }

  suspend fun saveFleetVehicle(vehicle: FleetVehicle): Result<String> {
    requireUserId()
    val isNew = vehicle.id.isBlank()
    val docRef =
      if (isNew) db.collection(COLLECTION_VEHICLES).document()
      else db.collection(COLLECTION_VEHICLES).document(vehicle.id)
    return try {
      if (isNew) {
        val payload = mapOf(
          "vehicleNumber" to vehicle.vehicleNumber.trim().uppercase(),
          "vehicleType" to vehicle.vehicleType,
          "driverName" to vehicle.driverName.trim(),
          "driverPhone" to vehicle.driverPhone.trim(),
          "availability" to vehicle.availability,
          "currentCity" to vehicle.currentCity.trim(),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "vehicleNumber" to vehicle.vehicleNumber.trim().uppercase(),
          "vehicleType" to vehicle.vehicleType,
          "driverName" to vehicle.driverName.trim(),
          "driverPhone" to vehicle.driverPhone.trim(),
          "availability" to vehicle.availability,
          "currentCity" to vehicle.currentCity.trim(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updates).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isNew) OperationType.CREATE else OperationType.UPDATE,
        docRef.path,
      )
      Result.failure(e)
    }
  }

  suspend fun updateVehicleAvailability(vehicleId: String, availability: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(COLLECTION_VEHICLES).document(vehicleId)
    return try {
      docRef
        .update(
          mapOf(
            "availability" to availability,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteFleetVehicle(vehicleId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(COLLECTION_VEHICLES).document(vehicleId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Drivers ---

  fun observeDrivers(): Flow<List<Driver>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(COLLECTION_DRIVERS)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Driver::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_DRIVERS, uid)
        }
    )
  }

  suspend fun saveDriver(driver: Driver): Result<String> {
    requireUserId()
    val isNew = driver.id.isBlank()
    val docRef =
      if (isNew) db.collection(COLLECTION_DRIVERS).document()
      else db.collection(COLLECTION_DRIVERS).document(driver.id)
    return try {
      if (isNew) {
        val payload = mapOf(
          "name" to driver.name.trim(),
          "mobileNumber" to driver.mobileNumber.trim(),
          "licenseNumber" to driver.licenseNumber.trim().uppercase(),
          "assignedVehicleId" to driver.assignedVehicleId.trim(),
          "assignedVehicleNumber" to driver.assignedVehicleNumber.trim().uppercase(),
          "availability" to driver.availability,
          "tripStatus" to driver.tripStatus,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "name" to driver.name.trim(),
          "mobileNumber" to driver.mobileNumber.trim(),
          "licenseNumber" to driver.licenseNumber.trim().uppercase(),
          "assignedVehicleId" to driver.assignedVehicleId.trim(),
          "assignedVehicleNumber" to driver.assignedVehicleNumber.trim().uppercase(),
          "availability" to driver.availability,
          "tripStatus" to driver.tripStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updates).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isNew) OperationType.CREATE else OperationType.UPDATE,
        docRef.path,
      )
      Result.failure(e)
    }
  }

  suspend fun updateDriverStatus(
    driverId: String,
    availability: String,
    tripStatus: String,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection(COLLECTION_DRIVERS).document(driverId)
    return try {
      docRef
        .update(
          mapOf(
            "availability" to availability,
            "tripStatus" to tripStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteDriver(driverId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(COLLECTION_DRIVERS).document(driverId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Payments ---

  fun observeCustomerPayments(userId: String): Flow<List<PaymentRecord>> = flow {
    emitAll(
      db.collection(COLLECTION_PAYMENTS)
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_PAYMENTS, userId)
        }
    )
  }

  fun observeAllPayments(): Flow<List<PaymentRecord>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(COLLECTION_PAYMENTS)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_PAYMENTS, uid)
        }
    )
  }

  suspend fun submitCustomerPayment(
    booking: Booking,
    amount: Double,
    method: String,
    referenceId: String,
    notes: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection(COLLECTION_PAYMENTS).document()
    val payload = mapOf(
      "bookingId" to booking.id,
      "bookingCode" to booking.bookingCode,
      "userId" to uid,
      "customerName" to booking.customerName,
      "amount" to amount,
      "method" to method,
      "status" to PaymentRecordStatus.PENDING_VERIFICATION.label,
      "referenceId" to referenceId.trim(),
      "notes" to notes.trim(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordAdminPayment(
    booking: Booking,
    amount: Double,
    method: String,
    paymentStatus: String,
    totalFareAmount: Double,
    referenceId: String,
    notes: String,
  ): Result<Unit> {
    requireUserId()
    val payDocRef = db.collection(COLLECTION_PAYMENTS).document()
    val bookingDocRef = db.collection(COLLECTION_BOOKINGS).document(booking.id)
    return try {
      val newPaidAmount = booking.paidAmount + amount
      val payPayload = mapOf(
        "bookingId" to booking.id,
        "bookingCode" to booking.bookingCode,
        "userId" to booking.userId,
        "customerName" to booking.customerName,
        "amount" to amount,
        "method" to method,
        "status" to PaymentRecordStatus.PAID.label,
        "referenceId" to referenceId.trim(),
        "notes" to notes.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      payDocRef.set(payPayload).await()
      val bookingUpdates = mutableMapOf<String, Any>(
        "paidAmount" to newPaidAmount,
        "paymentStatus" to paymentStatus,
        "paymentMethod" to method,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (totalFareAmount > 0) {
        bookingUpdates["totalFareAmount"] = totalFareAmount
        bookingUpdates["estimatedFare"] = "₹${totalFareAmount.toLong()}"
      }
      bookingDocRef.update(bookingUpdates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, payDocRef.path)
      Result.failure(e)
    }
  }

  suspend fun verifyPaymentByAdmin(
    payment: PaymentRecord,
    approved: Boolean,
    booking: Booking?,
  ): Result<Unit> {
    requireUserId()
    val payRef = db.collection(COLLECTION_PAYMENTS).document(payment.id)
    return try {
      val newPayStatus =
        if (approved) PaymentRecordStatus.PAID.label else PaymentRecordStatus.REJECTED.label
      payRef
        .update(
          mapOf(
            "status" to newPayStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      if (approved && booking != null) {
        val updatedPaid = booking.paidAmount + payment.amount
        val bookingPaymentStatus =
          if (booking.totalFareAmount > 0 && updatedPaid >= booking.totalFareAmount) {
            PaymentStatus.PAID.label
          } else {
            PaymentStatus.ADVANCE_PAID.label
          }
        db.collection(COLLECTION_BOOKINGS)
          .document(booking.id)
          .update(
            mapOf(
              "paidAmount" to updatedPaid,
              "paymentStatus" to bookingPaymentStatus,
              "paymentMethod" to payment.method,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, payRef.path)
      Result.failure(e)
    }
  }

  // --- Notifications ---

  fun observeUserNotifications(userId: String): Flow<List<AppNotification>> = flow {
    emitAll(
      db.collection(COLLECTION_NOTIFICATIONS)
        .whereEqualTo("recipientUserId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              AppNotification::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_NOTIFICATIONS, userId)
        }
    )
  }

  fun observeAdminNotifications(): Flow<List<AppNotification>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(COLLECTION_NOTIFICATIONS)
        .whereEqualTo("recipientUserId", ADMIN_RECIPIENT)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              AppNotification::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_NOTIFICATIONS, uid)
        }
    )
  }

  suspend fun sendCustomerNotification(
    recipientUserId: String,
    bookingId: String,
    bookingCode: String,
    type: String,
    title: String,
    message: String,
  ): Result<Unit> {
    val senderUid = requireUserId()
    return sendNotificationInternal(
      recipientUserId = recipientUserId,
      senderUserId = senderUid,
      bookingId = bookingId,
      bookingCode = bookingCode,
      type = type,
      title = title,
      message = message,
    )
  }

  private suspend fun sendNotificationInternal(
    recipientUserId: String,
    senderUserId: String,
    bookingId: String,
    bookingCode: String,
    type: String,
    title: String,
    message: String,
  ): Result<Unit> {
    val docRef = db.collection(COLLECTION_NOTIFICATIONS).document()
    val payload = mapOf(
      "recipientUserId" to recipientUserId,
      "senderUserId" to senderUserId,
      "bookingId" to bookingId,
      "bookingCode" to bookingCode,
      "type" to type,
      "title" to title.trim().take(120),
      "message" to message.trim().take(400),
      "isRead" to false,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun markNotificationRead(notificationId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(COLLECTION_NOTIFICATIONS).document(notificationId)
    return try {
      docRef.update(mapOf("isRead" to true)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Admins ---

  fun observeAdmins(): Flow<List<AdminUser>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection(COLLECTION_ADMINS)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logAndRethrowIfSignedIn(error, OperationType.LIST, COLLECTION_ADMINS, uid)
        }
    )
  }

  suspend fun addAdmin(uid: String, email: String, name: String): Result<Unit> {
    val currentAdminUid = requireUserId()
    val trimmedUid = uid.trim()
    val docRef = db.collection(COLLECTION_ADMINS).document(trimmedUid)
    val payload = mapOf(
      "uid" to trimmedUid,
      "email" to email.trim(),
      "name" to name.trim(),
      "addedBy" to currentAdminUid,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  private fun logAndRethrowIfSignedIn(
    error: Throwable,
    op: OperationType,
    path: String,
    expectedUid: String,
  ): Nothing {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(exceptionToLog, op, path)
    }
    throw error
  }

  companion object {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ADMINS = "admins"
    const val COLLECTION_VEHICLES = "vehicles"
    const val COLLECTION_DRIVERS = "drivers"
    const val COLLECTION_BOOKINGS = "bookings"
    const val COLLECTION_PAYMENTS = "payments"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val ADMIN_RECIPIENT = "ADMIN"
    const val BOOTSTRAPPED_ADMIN_EMAIL = "lraj65576@gmail.com"
  }
}
