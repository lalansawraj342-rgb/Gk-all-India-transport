package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminUser
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.FleetVehicle
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

  suspend fun saveUserProfile(
    name: String,
    mobileNumber: String,
    email: String,
    city: String,
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

  suspend fun createBooking(booking: Booking): Result<String> {
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
        "specialInstructions" to booking.specialInstructions.trim(),
        // CRITICAL: Customers can NEVER receive an auto-confirmed booking; always New Booking.
        "status" to BookingStatus.NEW_BOOKING.label,
        "assignedVehicleId" to "",
        "assignedVehicleNumber" to "",
        "assignedDriverName" to "",
        "assignedDriverPhone" to "",
        "adminNotes" to "",
        "estimatedFare" to "",
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
    assignedDriverName: String? = null,
    assignedDriverPhone: String? = null,
    adminNotes: String? = null,
    estimatedFare: String? = null,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection(COLLECTION_BOOKINGS).document(bookingId)
    val updates = mutableMapOf<String, Any>(
      "status" to status,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (assignedVehicleId != null) updates["assignedVehicleId"] = assignedVehicleId.trim()
    if (assignedVehicleNumber != null) updates["assignedVehicleNumber"] = assignedVehicleNumber.trim()
    if (assignedDriverName != null) updates["assignedDriverName"] = assignedDriverName.trim()
    if (assignedDriverPhone != null) updates["assignedDriverPhone"] = assignedDriverPhone.trim()
    if (adminNotes != null) updates["adminNotes"] = adminNotes.trim()
    if (estimatedFare != null) updates["estimatedFare"] = estimatedFare.trim()

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
    const val COLLECTION_BOOKINGS = "bookings"
    const val BOOTSTRAPPED_ADMIN_EMAIL = "lraj65576@gmail.com"
  }
}
