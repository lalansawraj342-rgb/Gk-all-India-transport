package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.VehicleType
import com.example.data.preferences.LanguagePreferences
import com.example.data.repository.TransportRepository
import com.example.notifications.BookingNotificationHelper
import com.example.ui.admin.AdminAccessRestrictedScreen
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminFleetScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.ProfileRegistrationScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AppNotificationsDialog
import com.example.ui.components.SupportHelplineDialog
import com.example.ui.customer.BookVehicleScreen
import com.example.ui.customer.HomeScreen
import com.example.ui.customer.MyBookingsScreen
import com.example.ui.customer.ProfileScreen
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.HindiStrings
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GkTransportTheme
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.TransportViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class AppDestination {
  HOME,
  BOOK_VEHICLE,
  MY_BOOKINGS,
  PROFILE,
  ADMIN_DESK,
  ADMIN_FLEET,
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    BookingNotificationHelper.ensureChannel(this)
    setContent {
      GkTransportRoot()
    }
  }
}

@Composable
fun GkTransportRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val langPrefs = remember(context) { LanguagePreferences(context) }
  val langCode by langPrefs.languageFlow.collectAsStateWithLifecycle(initialValue = "en")
  val isHindi = langCode == "hi"
  val strings = if (isHindi) HindiStrings else EnglishStrings

  val toggleLanguage: () -> Unit = {
    scope.launch { langPrefs.setLanguage(if (isHindi) "en" else "hi") }
  }

  GkTransportTheme(isHindi = isHindi, darkTheme = false) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
      AuthScreen(
        strings = strings,
        onToggleLanguage = toggleLanguage,
        onAuthSuccess = {},
      )
    } else {
      key(user.uid) {
        AuthenticatedTransportShell(
          user = user,
          langCode = langCode,
          strings = strings,
          onToggleLanguage = toggleLanguage,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedTransportShell(
  user: FirebaseUser,
  langCode: String,
  strings: AppStrings,
  onToggleLanguage: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: TransportViewModel =
    viewModel(
      key = user.uid,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            TransportViewModel(
              repository = TransportRepository(db, Firebase.auth),
              currentUserId = user.uid,
              userEmail = user.email ?: "",
              isEmailVerified = user.isEmailVerified,
            )
          }
        },
    )

  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val customerBookingsState by viewModel.customerBookingsState.collectAsStateWithLifecycle()
  val customerPaymentsState by viewModel.customerPaymentsState.collectAsStateWithLifecycle()
  val customerNotificationsState by viewModel.customerNotificationsState.collectAsStateWithLifecycle()
  val latestCustomerNotification by viewModel.latestCustomerNotification.collectAsStateWithLifecycle()

  val adminBookingsState by viewModel.adminBookingsState.collectAsStateWithLifecycle()
  val fleetVehiclesState by viewModel.fleetVehiclesState.collectAsStateWithLifecycle()
  val driversState by viewModel.driversState.collectAsStateWithLifecycle()
  val adminCustomersState by viewModel.adminCustomersState.collectAsStateWithLifecycle()
  val adminPaymentsState by viewModel.adminPaymentsState.collectAsStateWithLifecycle()
  val adminNotificationsState by viewModel.adminNotificationsState.collectAsStateWithLifecycle()
  val adminsState by viewModel.adminsState.collectAsStateWithLifecycle()

  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()
  val actionError by viewModel.actionError.collectAsStateWithLifecycle()
  val incomingAlert by viewModel.newIncomingBookingAlert.collectAsStateWithLifecycle()

  LaunchedEffect(incomingAlert, isAdmin) {
    val alert = incomingAlert
    if (isAdmin && alert != null) {
      BookingNotificationHelper.notifyNewBooking(context, alert, strings.isHindi)
    }
  }

  LaunchedEffect(latestCustomerNotification) {
    val notif = latestCustomerNotification
    if (notif != null) {
      BookingNotificationHelper.notifyAppNotification(context, notif)
      viewModel.dismissCustomerNotificationAlert()
    }
  }

  val handleSignOut: () -> Unit = {
    signOutUser(credentialManager = credentialManager, scope = scope)
  }

  when (val profileState = userProfileState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = EmeraldPrimary)
      }
    }
    is UiState.Error -> {
      ProfileRegistrationScreen(
        strings = strings,
        initialName = user.displayName ?: "",
        userEmail = user.email ?: "",
        isSaving = isActionLoading,
        errorMessage = actionError ?: profileState.message,
        onToggleLanguage = onToggleLanguage,
        onSignOut = handleSignOut,
        onSaveProfile = { name, mobile, city, address, altPhone ->
          viewModel.saveUserProfile(name, mobile, city, address, altPhone, langCode)
        },
      )
    }
    is UiState.Success -> {
      val profile = profileState.data
      if (profile == null || profile.mobileNumber.filter { it.isDigit() }.length < 10) {
        ProfileRegistrationScreen(
          strings = strings,
          initialName = profile?.name?.ifBlank { user.displayName ?: "" } ?: (user.displayName ?: ""),
          userEmail = user.email ?: "",
          isSaving = isActionLoading,
          errorMessage = actionError,
          onToggleLanguage = onToggleLanguage,
          onSignOut = handleSignOut,
          onSaveProfile = { name, mobile, city, address, altPhone ->
            viewModel.saveUserProfile(name, mobile, city, address, altPhone, langCode)
          },
        )
      } else {
        var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
        var adminMode by rememberSaveable { mutableStateOf(false) }
        var preselectedVehicle by remember { mutableStateOf<VehicleType?>(null) }
        var myBookingsInitialTab by rememberSaveable { mutableIntStateOf(0) }
        var trackingTargetBooking by remember { mutableStateOf<Booking?>(null) }
        var showSupportDialog by rememberSaveable { mutableStateOf(false) }
        var showNotificationsDialog by rememberSaveable { mutableStateOf(false) }

        val pendingAdminCount =
          (adminBookingsState as? UiState.Success)?.data?.count {
            BookingStatus.fromLabel(it.status) == BookingStatus.PENDING
          } ?: 0

        val customerNotifs = (customerNotificationsState as? UiState.Success)?.data ?: emptyList()
        val adminNotifs = (adminNotificationsState as? UiState.Success)?.data ?: emptyList()
        val activeNotifs = if (adminMode && isAdmin) adminNotifs else customerNotifs
        val unreadNotifCount = activeNotifs.count { !it.isRead }

        BackHandler(enabled = currentDestination != AppDestination.HOME) {
          currentDestination =
            if (adminMode && currentDestination == AppDestination.ADMIN_FLEET) {
              AppDestination.ADMIN_DESK
            } else {
              adminMode = false
              AppDestination.HOME
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val isExpanded = maxWidth >= 600.dp

          Scaffold(
            topBar = {
              TopAppBar(
                title = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.LocalShipping,
                      contentDescription = null,
                      tint = EmeraldPrimary,
                      modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = strings.brandTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite,
                      )
                    }
                  }
                },
                actions = {
                  IconButton(
                    onClick = { showNotificationsDialog = true },
                    modifier = Modifier.testTag("topbar_notifications_btn"),
                  ) {
                    BadgedBox(
                      badge = {
                        if (unreadNotifCount > 0) {
                          Badge { Text(unreadNotifCount.toString()) }
                        }
                      }
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = strings.notificationsTitle,
                        tint = PureWhite,
                      )
                    }
                  }

                  Surface(
                    onClick = {
                      adminMode = !adminMode
                      currentDestination =
                        if (adminMode) AppDestination.ADMIN_DESK else AppDestination.HOME
                    },
                    color = if (adminMode) EmeraldPrimary else PureWhite.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(end = 4.dp).testTag("mode_switch_button"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = if (adminMode) strings.switchToCustomer else strings.switchToAdmin,
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                      )
                      if (isAdmin && !adminMode && pendingAdminCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge { Text(pendingAdminCount.toString()) }
                      }
                    }
                  }

                  IconButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.testTag("topbar_language_toggle"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Translate,
                      contentDescription = strings.languageButtonLabel,
                      tint = PureWhite,
                    )
                  }
                },
                colors =
                  TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDeep,
                    titleContentColor = PureWhite,
                  ),
              )
            },
            bottomBar = {
              if (!isExpanded) {
                NavigationBar(
                  containerColor = PureWhite,
                  windowInsets = WindowInsets.navigationBars,
                ) {
                  if (!adminMode) {
                    CustomerNavItems(
                      strings = strings,
                      currentDestination = currentDestination,
                      onSelect = { dest ->
                        if (dest != AppDestination.MY_BOOKINGS) myBookingsInitialTab = 0
                        currentDestination = dest
                      },
                    )
                  } else {
                    AdminNavItems(
                      strings = strings,
                      currentDestination = currentDestination,
                      pendingAdminCount = pendingAdminCount,
                      onSelect = { currentDestination = it },
                    )
                  }
                }
              }
            },
          ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
              if (isExpanded) {
                NavigationRail(
                  containerColor = PureWhite,
                  modifier = Modifier.fillMaxHeight(),
                ) {
                  if (!adminMode) {
                    NavigationRailItem(
                      selected = currentDestination == AppDestination.HOME,
                      onClick = { currentDestination = AppDestination.HOME },
                      icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                      label = { Text(strings.tabHome) },
                    )
                    NavigationRailItem(
                      selected = currentDestination == AppDestination.BOOK_VEHICLE,
                      onClick = { currentDestination = AppDestination.BOOK_VEHICLE },
                      icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
                      label = { Text(strings.tabBookVehicle) },
                    )
                    NavigationRailItem(
                      selected = currentDestination == AppDestination.MY_BOOKINGS,
                      onClick = {
                        myBookingsInitialTab = 0
                        currentDestination = AppDestination.MY_BOOKINGS
                      },
                      icon = { Icon(Icons.Filled.ListAlt, contentDescription = null) },
                      label = { Text(strings.tabMyBookings) },
                    )
                    NavigationRailItem(
                      selected = currentDestination == AppDestination.PROFILE,
                      onClick = { currentDestination = AppDestination.PROFILE },
                      icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                      label = { Text(strings.tabProfile) },
                    )
                  } else {
                    NavigationRailItem(
                      selected = currentDestination == AppDestination.ADMIN_DESK,
                      onClick = { currentDestination = AppDestination.ADMIN_DESK },
                      icon = {
                        BadgedBox(
                          badge = {
                            if (pendingAdminCount > 0) {
                              Badge { Text(pendingAdminCount.toString()) }
                            }
                          }
                        ) {
                          Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                        }
                      },
                      label = { Text(strings.tabAdminPanel) },
                    )
                    NavigationRailItem(
                      selected = currentDestination == AppDestination.ADMIN_FLEET,
                      onClick = { currentDestination = AppDestination.ADMIN_FLEET },
                      icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                      label = { Text(strings.tabFleet) },
                    )
                  }
                }
              }

              Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                when (currentDestination) {
                  AppDestination.HOME -> {
                    HomeScreen(
                      strings = strings,
                      userProfile = profile,
                      bookingsState = customerBookingsState,
                      onBookVehicleClick = { vehicleType ->
                        preselectedVehicle = vehicleType
                        currentDestination = AppDestination.BOOK_VEHICLE
                      },
                      onMyBookingsClick = {
                        myBookingsInitialTab = 0
                        currentDestination = AppDestination.MY_BOOKINGS
                      },
                      onBookingHistoryClick = {
                        myBookingsInitialTab = 1
                        currentDestination = AppDestination.MY_BOOKINGS
                      },
                      onTrackVehicleClick = { activeBooking ->
                        myBookingsInitialTab = 0
                        trackingTargetBooking = activeBooking
                        currentDestination = AppDestination.MY_BOOKINGS
                      },
                      onPaymentsClick = {
                        myBookingsInitialTab = 2
                        currentDestination = AppDestination.MY_BOOKINGS
                      },
                      onSupportClick = { showSupportDialog = true },
                    )
                  }
                  AppDestination.BOOK_VEHICLE -> {
                    BookVehicleScreen(
                      strings = strings,
                      userProfile = profile,
                      preselectedVehicle = preselectedVehicle,
                      isSubmitting = isActionLoading,
                      errorMessage = actionError,
                      onSubmitBooking = { booking, onSuccess ->
                        viewModel.submitBooking(booking) { _, bookingCode ->
                          onSuccess(bookingCode)
                        }
                      },
                      onNavigateToMyBookings = {
                        myBookingsInitialTab = 0
                        currentDestination = AppDestination.MY_BOOKINGS
                      },
                    )
                  }
                  AppDestination.MY_BOOKINGS -> {
                    MyBookingsScreen(
                      strings = strings,
                      bookingsState = customerBookingsState,
                      paymentsState = customerPaymentsState,
                      initialTabIndex = myBookingsInitialTab,
                      initialDetailBooking = trackingTargetBooking,
                      onClearInitialDetail = { trackingTargetBooking = null },
                      onCancelBooking = { id -> viewModel.cancelCustomerBooking(id) },
                      onSubmitPayment = { booking, amount, method, ref, notes ->
                        viewModel.submitCustomerPayment(booking, amount, method, ref, notes)
                      },
                      onBookNewClick = {
                        preselectedVehicle = null
                        currentDestination = AppDestination.BOOK_VEHICLE
                      },
                      onSupportClick = { showSupportDialog = true },
                    )
                  }
                  AppDestination.PROFILE -> {
                    ProfileScreen(
                      strings = strings,
                      userId = user.uid,
                      userEmail = user.email ?: "",
                      userProfile = profile,
                      isSaving = isActionLoading,
                      onSaveProfile = { name, mobile, city, address, altPhone, onDone ->
                        viewModel.saveUserProfile(
                          name = name,
                          mobileNumber = mobile,
                          city = city,
                          address = address,
                          alternatePhone = altPhone,
                          preferredLanguage = langCode,
                          onSuccess = onDone,
                        )
                      },
                      onToggleLanguage = onToggleLanguage,
                      onSignOut = handleSignOut,
                    )
                  }
                  AppDestination.ADMIN_DESK -> {
                    if (!isAdmin) {
                      AdminAccessRestrictedScreen(
                        strings = strings,
                        currentUid = user.uid,
                        currentEmail = user.email ?: "",
                        onReturnCustomer = {
                          adminMode = false
                          currentDestination = AppDestination.HOME
                        },
                      )
                    } else {
                      AdminDashboardScreen(
                        strings = strings,
                        bookingsState = adminBookingsState,
                        vehiclesState = fleetVehiclesState,
                        driversState = driversState,
                        incomingAlert = incomingAlert,
                        onDismissAlert = { viewModel.dismissIncomingAlert() },
                        onUpdateStatus = { booking, newStatus, notes ->
                          viewModel.adminUpdateBookingStatus(booking, newStatus, notes)
                        },
                        onAssignVehicle = { booking, fleetId, number, driverId, dName, dPhone, fare, totalFare, notes ->
                          viewModel.adminAssignVehicle(
                            booking = booking,
                            fleetVehicleId = fleetId,
                            vehicleNumber = number,
                            driverId = driverId,
                            driverName = dName,
                            driverPhone = dPhone,
                            estimatedFare = fare,
                            totalFareAmount = totalFare,
                            adminNotes = notes,
                          )
                        },
                        onRecordPayment = { booking, amount, method, pStatus, totalFare, ref, notes ->
                          viewModel.adminRecordPayment(
                            booking = booking,
                            amount = amount,
                            method = method,
                            paymentStatus = pStatus,
                            totalFareAmount = totalFare,
                            referenceId = ref,
                            notes = notes,
                          )
                        },
                        onSendNotification = { booking, title, message ->
                          viewModel.adminSendCustomNotification(booking, title, message)
                        },
                      )
                    }
                  }
                  AppDestination.ADMIN_FLEET -> {
                    if (!isAdmin) {
                      AdminAccessRestrictedScreen(
                        strings = strings,
                        currentUid = user.uid,
                        currentEmail = user.email ?: "",
                        onReturnCustomer = {
                          adminMode = false
                          currentDestination = AppDestination.HOME
                        },
                      )
                    } else {
                      AdminFleetScreen(
                        strings = strings,
                        vehiclesState = fleetVehiclesState,
                        driversState = driversState,
                        customersState = adminCustomersState,
                        paymentsState = adminPaymentsState,
                        bookingsState = adminBookingsState,
                        adminsState = adminsState,
                        onSaveVehicle = { v -> viewModel.saveFleetVehicle(v) },
                        onUpdateAvailability = { id, av ->
                          viewModel.updateFleetAvailability(id, av)
                        },
                        onDeleteVehicle = { id -> viewModel.deleteFleetVehicle(id) },
                        onSaveDriver = { d -> viewModel.saveDriver(d) },
                        onUpdateDriverStatus = { id, av, ts ->
                          viewModel.updateDriverStatus(id, av, ts)
                        },
                        onDeleteDriver = { id -> viewModel.deleteDriver(id) },
                        onVerifyPayment = { payment, approved, booking ->
                          viewModel.adminVerifyPayment(payment, approved, booking)
                        },
                        onAddAdmin = { uid, email, name -> viewModel.addAdmin(uid, email, name) },
                      )
                    }
                  }
                }
              }
            }
          }

          if (showSupportDialog) {
            SupportHelplineDialog(
              strings = strings,
              context = context,
              onDismiss = { showSupportDialog = false },
            )
          }

          if (showNotificationsDialog) {
            AppNotificationsDialog(
              strings = strings,
              notifications = activeNotifs,
              onMarkRead = { id -> viewModel.markNotificationRead(id) },
              onDismiss = { showNotificationsDialog = false },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CustomerNavItems(
  strings: AppStrings,
  currentDestination: AppDestination,
  onSelect: (AppDestination) -> Unit,
) {
  val itemColors =
    NavigationBarItemDefaults.colors(
      selectedIconColor = EmeraldPrimary,
      selectedTextColor = EmeraldPrimary,
      indicatorColor = EmeraldLightContainer,
    )
  NavigationBarItem(
    selected = currentDestination == AppDestination.HOME,
    onClick = { onSelect(AppDestination.HOME) },
    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
    label = { Text(strings.tabHome) },
    colors = itemColors,
  )
  NavigationBarItem(
    selected = currentDestination == AppDestination.BOOK_VEHICLE,
    onClick = { onSelect(AppDestination.BOOK_VEHICLE) },
    icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
    label = { Text(strings.tabBookVehicle) },
    colors = itemColors,
  )
  NavigationBarItem(
    selected = currentDestination == AppDestination.MY_BOOKINGS,
    onClick = { onSelect(AppDestination.MY_BOOKINGS) },
    icon = { Icon(Icons.Filled.ListAlt, contentDescription = null) },
    label = { Text(strings.tabMyBookings) },
    colors = itemColors,
  )
  NavigationBarItem(
    selected = currentDestination == AppDestination.PROFILE,
    onClick = { onSelect(AppDestination.PROFILE) },
    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
    label = { Text(strings.tabProfile) },
    colors = itemColors,
  )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AdminNavItems(
  strings: AppStrings,
  currentDestination: AppDestination,
  pendingAdminCount: Int,
  onSelect: (AppDestination) -> Unit,
) {
  val itemColors =
    NavigationBarItemDefaults.colors(
      selectedIconColor = EmeraldPrimary,
      selectedTextColor = EmeraldPrimary,
      indicatorColor = EmeraldLightContainer,
    )
  NavigationBarItem(
    selected = currentDestination == AppDestination.ADMIN_DESK,
    onClick = { onSelect(AppDestination.ADMIN_DESK) },
    icon = {
      BadgedBox(
        badge = {
          if (pendingAdminCount > 0) {
            Badge { Text(pendingAdminCount.toString()) }
          }
        }
      ) {
        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
      }
    },
    label = { Text(strings.tabAdminPanel) },
    colors = itemColors,
  )
  NavigationBarItem(
    selected = currentDestination == AppDestination.ADMIN_FLEET,
    onClick = { onSelect(AppDestination.ADMIN_FLEET) },
    icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
    label = { Text(strings.tabFleet) },
    colors = itemColors,
  )
}
