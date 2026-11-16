package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminUser
import com.example.data.model.AppNotification
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.Driver
import com.example.data.model.FleetVehicle
import com.example.data.model.NotificationType
import com.example.data.model.PaymentRecord
import com.example.data.model.UserProfile
import com.example.data.repository.TransportRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransportViewModel(
  private val repository: TransportRepository,
  val currentUserId: String,
  val userEmail: String,
  private val isEmailVerified: Boolean,
) : ViewModel() {

  private val _isAdmin = MutableStateFlow(false)
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _isActionLoading = MutableStateFlow(false)
  val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

  private val _actionError = MutableStateFlow<String?>(null)
  val actionError: StateFlow<String?> = _actionError.asStateFlow()

  private var adminBookingsInitialized = false
  private val seenBookingIds = mutableSetOf<String>()
  private val _newIncomingBookingAlert = MutableStateFlow<Booking?>(null)
  val newIncomingBookingAlert: StateFlow<Booking?> = _newIncomingBookingAlert.asStateFlow()

  private var customerNotifsInitialized = false
  private val seenCustomerNotifIds = mutableSetOf<String>()
  private val _latestCustomerNotification = MutableStateFlow<AppNotification?>(null)
  val latestCustomerNotification: StateFlow<AppNotification?> = _latestCustomerNotification.asStateFlow()

  init {
    viewModelScope.launch {
      _isAdmin.value = repository.isUserAdmin(currentUserId, userEmail, isEmailVerified)
    }
  }

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    repository
      .observeUserProfile(currentUserId)
      .asUiState("Failed to load profile")
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val customerBookingsState: StateFlow<UiState<List<Booking>>> =
    repository
      .observeCustomerBookings(currentUserId)
      .asUiState("Failed to load your bookings")
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val customerPaymentsState: StateFlow<UiState<List<PaymentRecord>>> =
    repository
      .observeCustomerPayments(currentUserId)
      .asUiState("Failed to load payments")
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val customerNotificationsState: StateFlow<UiState<List<AppNotification>>> =
    repository
      .observeUserNotifications(currentUserId)
      .onEach { list ->
        if (!customerNotifsInitialized) {
          seenCustomerNotifIds.addAll(list.map { it.id })
          customerNotifsInitialized = true
        } else {
          val newlyArrived = list.firstOrNull { it.id !in seenCustomerNotifIds && !it.isRead }
          seenCustomerNotifIds.addAll(list.map { it.id })
          if (newlyArrived != null) {
            _latestCustomerNotification.value = newlyArrived
          }
        }
      }
      .asUiState("Failed to load notifications")
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminBookingsState: StateFlow<UiState<List<Booking>>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (!admin) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeAllBookings()
            .onEach { bookings ->
              if (!adminBookingsInitialized) {
                seenBookingIds.addAll(bookings.map { it.id })
                adminBookingsInitialized = true
              } else {
                val newlyArrived =
                  bookings.firstOrNull {
                    it.id !in seenBookingIds && it.status == BookingStatus.PENDING.label
                  }
                seenBookingIds.addAll(bookings.map { it.id })
                if (newlyArrived != null) {
                  _newIncomingBookingAlert.value = newlyArrived
                }
              }
            }
            .asUiState("Failed to load admin bookings")
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val fleetVehiclesState: StateFlow<UiState<List<FleetVehicle>>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeFleetVehicles().asUiState("Failed to load fleet vehicles")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val driversState: StateFlow<UiState<List<Driver>>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeDrivers().asUiState("Failed to load drivers")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminCustomersState: StateFlow<UiState<List<UserProfile>>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeAllCustomers().asUiState("Failed to load customers")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminPaymentsState: StateFlow<UiState<List<PaymentRecord>>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeAllPayments().asUiState("Failed to load payment records")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminNotificationsState: StateFlow<UiState<List<AppNotification>>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeAdminNotifications().asUiState("Failed to load admin notifications")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminsState: StateFlow<UiState<List<AdminUser>>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (!admin) flowOf(UiState.Success(emptyList()))
        else repository.observeAdmins().asUiState("Failed to load admin team")
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  fun saveUserProfile(
    name: String,
    mobileNumber: String,
    city: String,
    address: String,
    alternatePhone: String,
    preferredLanguage: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result =
        repository.saveUserProfile(
          name = name,
          mobileNumber = mobileNumber,
          email = userEmail,
          city = city,
          address = address,
          alternatePhone = alternatePhone,
          preferredLanguage = preferredLanguage,
        )
      _isActionLoading.value = false
      result
        .onSuccess { onSuccess() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to save profile" }
    }
  }

  fun submitBooking(booking: Booking, onSuccess: (bookingId: String, bookingCode: String) -> Unit) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result = repository.createBooking(booking)
      _isActionLoading.value = false
      result
        .onSuccess { (docId, bookingCode) -> onSuccess(docId, bookingCode) }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to submit booking" }
    }
  }

  fun cancelCustomerBooking(bookingId: String) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result = repository.cancelCustomerBooking(bookingId)
      _isActionLoading.value = false
      result.onFailure { _actionError.value = it.localizedMessage ?: "Failed to cancel booking" }
    }
  }

  fun submitCustomerPayment(
    booking: Booking,
    amount: Double,
    method: String,
    referenceId: String,
    notes: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result = repository.submitCustomerPayment(booking, amount, method, referenceId, notes)
      _isActionLoading.value = false
      result
        .onSuccess { onSuccess() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to submit payment" }
    }
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch {
      repository.markNotificationRead(notificationId)
    }
  }

  fun adminUpdateBookingStatus(
    booking: Booking,
    newStatus: String,
    adminNotes: String? = null,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result =
        repository.updateBookingByAdmin(
          bookingId = booking.id,
          status = newStatus,
          adminNotes = adminNotes,
        )
      if (result.isSuccess) {
        val notifType =
          when (BookingStatus.fromLabel(newStatus)) {
            BookingStatus.ACCEPTED -> NotificationType.BOOKING_ACCEPTED.code
            BookingStatus.VEHICLE_ASSIGNED, BookingStatus.DRIVER_ASSIGNED ->
              NotificationType.VEHICLE_ASSIGNED.code
            BookingStatus.DELIVERED -> NotificationType.DELIVERY_COMPLETED.code
            else -> NotificationType.ADMIN_MESSAGE.code
          }
        repository.sendCustomerNotification(
          recipientUserId = booking.userId,
          bookingId = booking.id,
          bookingCode = booking.bookingCode,
          type = notifType,
          title = "Booking ${booking.bookingCode}: $newStatus",
          message =
            if (!adminNotes.isNullOrBlank()) adminNotes
            else "Your ${booking.vehicleType} booking status is now $newStatus.",
        )
      }
      _isActionLoading.value = false
      result
        .onSuccess { onDone() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to update status" }
    }
  }

  fun adminAssignVehicle(
    booking: Booking,
    fleetVehicleId: String?,
    vehicleNumber: String,
    driverId: String?,
    driverName: String,
    driverPhone: String,
    estimatedFare: String,
    totalFareAmount: Double,
    adminNotes: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val fareLabel =
        if (estimatedFare.isBlank() && totalFareAmount > 0) "₹${totalFareAmount.toLong()}"
        else estimatedFare
      val targetStatus =
        if (driverName.isNotBlank() && vehicleNumber.isNotBlank()) BookingStatus.DRIVER_ASSIGNED.label
        else BookingStatus.VEHICLE_ASSIGNED.label
      val result =
        repository.updateBookingByAdmin(
          bookingId = booking.id,
          status = targetStatus,
          assignedVehicleId = fleetVehicleId ?: "",
          assignedVehicleNumber = vehicleNumber,
          assignedDriverId = driverId ?: "",
          assignedDriverName = driverName,
          assignedDriverPhone = driverPhone,
          estimatedFare = fareLabel,
          totalFareAmount = if (totalFareAmount > 0) totalFareAmount else null,
          adminNotes = adminNotes,
        )
      if (result.isSuccess) {
        if (!fleetVehicleId.isNullOrBlank()) {
          repository.updateVehicleAvailability(fleetVehicleId, "On Trip")
        }
        if (!driverId.isNullOrBlank()) {
          repository.updateDriverStatus(driverId, "On Trip", "En Route to Pickup")
        }
        repository.sendCustomerNotification(
          recipientUserId = booking.userId,
          bookingId = booking.id,
          bookingCode = booking.bookingCode,
          type = NotificationType.VEHICLE_ASSIGNED.code,
          title = "Vehicle Assigned (${booking.bookingCode})",
          message = "Truck $vehicleNumber • Driver $driverName ($driverPhone) assigned to your trip.",
        )
      }
      _isActionLoading.value = false
      result
        .onSuccess { onDone() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to assign vehicle" }
    }
  }

  fun adminRecordPayment(
    booking: Booking,
    amount: Double,
    method: String,
    paymentStatus: String,
    totalFareAmount: Double,
    referenceId: String,
    notes: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result =
        repository.recordAdminPayment(
          booking = booking,
          amount = amount,
          method = method,
          paymentStatus = paymentStatus,
          totalFareAmount = totalFareAmount,
          referenceId = referenceId,
          notes = notes,
        )
      _isActionLoading.value = false
      result
        .onSuccess { onDone() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to record payment" }
    }
  }

  fun adminVerifyPayment(payment: PaymentRecord, approved: Boolean, booking: Booking?) {
    viewModelScope.launch {
      repository.verifyPaymentByAdmin(payment, approved, booking)
    }
  }

  fun adminSendCustomNotification(
    booking: Booking,
    title: String,
    message: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isActionLoading.value = true
      val result =
        repository.sendCustomerNotification(
          recipientUserId = booking.userId,
          bookingId = booking.id,
          bookingCode = booking.bookingCode,
          type = NotificationType.ADMIN_MESSAGE.code,
          title = title,
          message = message,
        )
      _isActionLoading.value = false
      result.onSuccess { onDone() }
    }
  }

  fun saveFleetVehicle(vehicle: FleetVehicle, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result = repository.saveFleetVehicle(vehicle)
      _isActionLoading.value = false
      result
        .onSuccess { onDone() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to save vehicle" }
    }
  }

  fun updateFleetAvailability(vehicleId: String, availability: String) {
    viewModelScope.launch {
      repository.updateVehicleAvailability(vehicleId, availability)
    }
  }

  fun deleteFleetVehicle(vehicleId: String) {
    viewModelScope.launch {
      repository.deleteFleetVehicle(vehicleId)
    }
  }

  fun saveDriver(driver: Driver, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result = repository.saveDriver(driver)
      _isActionLoading.value = false
      result
        .onSuccess { onDone() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to save driver" }
    }
  }

  fun updateDriverStatus(driverId: String, availability: String, tripStatus: String) {
    viewModelScope.launch {
      repository.updateDriverStatus(driverId, availability, tripStatus)
    }
  }

  fun deleteDriver(driverId: String) {
    viewModelScope.launch {
      repository.deleteDriver(driverId)
    }
  }

  fun addAdmin(uid: String, email: String, name: String, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result = repository.addAdmin(uid, email, name)
      _isActionLoading.value = false
      result
        .onSuccess { onDone() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to add admin" }
    }
  }

  fun dismissIncomingAlert() {
    _newIncomingBookingAlert.value = null
  }

  fun dismissCustomerNotificationAlert() {
    _latestCustomerNotification.value = null
  }

  private fun <T> Flow<T>.asUiState(fallbackMessage: String): Flow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackMessage, rootError)
        emit(UiState.Error(rootError.localizedMessage ?: fallbackMessage))
      }

  private companion object {
    const val TAG = "TransportVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
