package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminUser
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.FleetVehicle
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
import kotlinx.coroutines.flow.flow
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

  private val _lastSubmittedBookingCode = MutableStateFlow<String?>(null)
  val lastSubmittedBookingCode: StateFlow<String?> = _lastSubmittedBookingCode.asStateFlow()

  // Tracks previously seen booking IDs in Admin mode so only genuinely new arrivals alert
  private var adminBookingsInitialized = false
  private val seenBookingIds = mutableSetOf<String>()

  private val _newIncomingBookingAlert = MutableStateFlow<Booking?>(null)
  val newIncomingBookingAlert: StateFlow<Booking?> = _newIncomingBookingAlert.asStateFlow()

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
                    it.id !in seenBookingIds && it.status == BookingStatus.NEW_BOOKING.label
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

  fun refreshAdminCheck() {
    viewModelScope.launch {
      _isAdmin.value = repository.isUserAdmin(currentUserId, userEmail, isEmailVerified)
    }
  }

  fun saveUserProfile(
    name: String,
    mobileNumber: String,
    city: String,
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
          preferredLanguage = preferredLanguage,
        )
      _isActionLoading.value = false
      result
        .onSuccess { onSuccess() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to save profile" }
    }
  }

  fun submitBooking(booking: Booking, onSuccess: (String) -> Unit) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result = repository.createBooking(booking)
      _isActionLoading.value = false
      result
        .onSuccess { docId ->
          _lastSubmittedBookingCode.value = docId
          onSuccess(docId)
        }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to submit booking" }
    }
  }

  fun clearSubmittedBookingFlag() {
    _lastSubmittedBookingCode.value = null
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

  fun adminUpdateBookingStatus(
    bookingId: String,
    newStatus: String,
    adminNotes: String? = null,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result =
        repository.updateBookingByAdmin(
          bookingId = bookingId,
          status = newStatus,
          adminNotes = adminNotes,
        )
      _isActionLoading.value = false
      result
        .onSuccess { onDone() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to update status" }
    }
  }

  fun adminAssignVehicle(
    bookingId: String,
    fleetVehicleId: String?,
    vehicleNumber: String,
    driverName: String,
    driverPhone: String,
    estimatedFare: String,
    adminNotes: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isActionLoading.value = true
      _actionError.value = null
      val result =
        repository.updateBookingByAdmin(
          bookingId = bookingId,
          status = BookingStatus.VEHICLE_ASSIGNED.label,
          assignedVehicleId = fleetVehicleId ?: "",
          assignedVehicleNumber = vehicleNumber,
          assignedDriverName = driverName,
          assignedDriverPhone = driverPhone,
          estimatedFare = estimatedFare,
          adminNotes = adminNotes,
        )
      if (result.isSuccess && !fleetVehicleId.isNullOrBlank()) {
        repository.updateVehicleAvailability(fleetVehicleId, "On Trip")
      }
      _isActionLoading.value = false
      result
        .onSuccess { onDone() }
        .onFailure { _actionError.value = it.localizedMessage ?: "Failed to assign vehicle" }
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

  fun clearError() {
    _actionError.value = null
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
