package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.example.data.model.BookingStatus
import com.example.data.preferences.LanguagePreferences
import com.example.data.repository.TransportRepository
import com.example.notifications.BookingNotificationHelper
import com.example.ui.admin.AdminAccessRestrictedScreen
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminFleetScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.ProfileRegistrationScreen
import com.example.ui.auth.signOutUser
import com.example.ui.customer.BookVehicleScreen
import com.example.ui.customer.MyBookingsScreen
import com.example.ui.customer.ProfileScreen
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
  strings: com.example.ui.i18n.AppStrings,
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
  val adminBookingsState by viewModel.adminBookingsState.collectAsStateWithLifecycle()
  val fleetVehiclesState by viewModel.fleetVehiclesState.collectAsStateWithLifecycle()
  val adminsState by viewModel.adminsState.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()
  val actionError by viewModel.actionError.collectAsStateWithLifecycle()
  val incomingAlert by viewModel.newIncomingBookingAlert.collectAsStateWithLifecycle()

  // Trigger instant Android notification when Admin receives a new booking in real time
  LaunchedEffect(incomingAlert, isAdmin) {
    val alert = incomingAlert
    if (isAdmin && alert != null) {
      BookingNotificationHelper.notifyNewBooking(context, alert, strings.isHindi)
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
        onSaveProfile = { name, mobile, city ->
          viewModel.saveUserProfile(name, mobile, city, langCode)
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
          onSaveProfile = { name, mobile, city ->
            viewModel.saveUserProfile(name, mobile, city, langCode)
          },
        )
      } else {
        var currentDestination by rememberSaveable { mutableStateOf(AppDestination.BOOK_VEHICLE) }
        var adminMode by rememberSaveable { mutableStateOf(false) }

        val newAdminCount =
          (adminBookingsState as? UiState.Success)?.data?.count {
            it.status == BookingStatus.NEW_BOOKING.label
          } ?: 0

        BackHandler(enabled = currentDestination != AppDestination.BOOK_VEHICLE) {
          currentDestination =
            if (adminMode && currentDestination == AppDestination.ADMIN_FLEET) {
              AppDestination.ADMIN_DESK
            } else {
              adminMode = false
              AppDestination.BOOK_VEHICLE
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
                  Surface(
                    onClick = {
                      adminMode = !adminMode
                      currentDestination =
                        if (adminMode) AppDestination.ADMIN_DESK else AppDestination.BOOK_VEHICLE
                    },
                    color = if (adminMode) EmeraldPrimary else PureWhite.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(end = 6.dp).testTag("mode_switch_button"),
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
                      if (isAdmin && !adminMode && newAdminCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge { Text(newAdminCount.toString()) }
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
                    NavigationBarItem(
                      selected = currentDestination == AppDestination.BOOK_VEHICLE,
                      onClick = { currentDestination = AppDestination.BOOK_VEHICLE },
                      icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
                      label = { Text(strings.tabBookVehicle) },
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = EmeraldPrimary,
                          selectedTextColor = EmeraldPrimary,
                          indicatorColor = EmeraldLightContainer,
                        ),
                    )
                    NavigationBarItem(
                      selected = currentDestination == AppDestination.MY_BOOKINGS,
                      onClick = { currentDestination = AppDestination.MY_BOOKINGS },
                      icon = { Icon(Icons.Filled.ListAlt, contentDescription = null) },
                      label = { Text(strings.tabMyBookings) },
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = EmeraldPrimary,
                          selectedTextColor = EmeraldPrimary,
                          indicatorColor = EmeraldLightContainer,
                        ),
                    )
                    NavigationBarItem(
                      selected = currentDestination == AppDestination.PROFILE,
                      onClick = { currentDestination = AppDestination.PROFILE },
                      icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                      label = { Text(strings.tabProfile) },
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = EmeraldPrimary,
                          selectedTextColor = EmeraldPrimary,
                          indicatorColor = EmeraldLightContainer,
                        ),
                    )
                  } else {
                    NavigationBarItem(
                      selected = currentDestination == AppDestination.ADMIN_DESK,
                      onClick = { currentDestination = AppDestination.ADMIN_DESK },
                      icon = {
                        BadgedBox(
                          badge = {
                            if (newAdminCount > 0) {
                              Badge { Text(newAdminCount.toString()) }
                            }
                          }
                        ) {
                          Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                        }
                      },
                      label = { Text(strings.tabAdminPanel) },
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = EmeraldPrimary,
                          selectedTextColor = EmeraldPrimary,
                          indicatorColor = EmeraldLightContainer,
                        ),
                    )
                    NavigationBarItem(
                      selected = currentDestination == AppDestination.ADMIN_FLEET,
                      onClick = { currentDestination = AppDestination.ADMIN_FLEET },
                      icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                      label = { Text(strings.tabFleet) },
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = EmeraldPrimary,
                          selectedTextColor = EmeraldPrimary,
                          indicatorColor = EmeraldLightContainer,
                        ),
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
                      selected = currentDestination == AppDestination.BOOK_VEHICLE,
                      onClick = { currentDestination = AppDestination.BOOK_VEHICLE },
                      icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
                      label = { Text(strings.tabBookVehicle) },
                    )
                    NavigationRailItem(
                      selected = currentDestination == AppDestination.MY_BOOKINGS,
                      onClick = { currentDestination = AppDestination.MY_BOOKINGS },
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
                            if (newAdminCount > 0) {
                              Badge { Text(newAdminCount.toString()) }
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
                  AppDestination.BOOK_VEHICLE -> {
                    BookVehicleScreen(
                      strings = strings,
                      userProfile = profile,
                      isSubmitting = isActionLoading,
                      errorMessage = actionError,
                      onSubmitBooking = { booking, onSuccess ->
                        viewModel.submitBooking(booking) { onSuccess() }
                      },
                      onNavigateToMyBookings = {
                        currentDestination = AppDestination.MY_BOOKINGS
                      },
                    )
                  }
                  AppDestination.MY_BOOKINGS -> {
                    MyBookingsScreen(
                      strings = strings,
                      bookingsState = customerBookingsState,
                      onCancelBooking = { id -> viewModel.cancelCustomerBooking(id) },
                      onBookNewClick = { currentDestination = AppDestination.BOOK_VEHICLE },
                    )
                  }
                  AppDestination.PROFILE -> {
                    ProfileScreen(
                      strings = strings,
                      userId = user.uid,
                      userEmail = user.email ?: "",
                      userProfile = profile,
                      isSaving = isActionLoading,
                      onSaveProfile = { name, mobile, city, onDone ->
                        viewModel.saveUserProfile(name, mobile, city, langCode, onDone)
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
                          currentDestination = AppDestination.BOOK_VEHICLE
                        },
                      )
                    } else {
                      AdminDashboardScreen(
                        strings = strings,
                        bookingsState = adminBookingsState,
                        vehiclesState = fleetVehiclesState,
                        incomingAlert = incomingAlert,
                        onDismissAlert = { viewModel.dismissIncomingAlert() },
                        onUpdateStatus = { id, newStatus, notes ->
                          viewModel.adminUpdateBookingStatus(id, newStatus, notes)
                        },
                        onAssignVehicle = { id, fleetId, number, dName, dPhone, fare, notes ->
                          viewModel.adminAssignVehicle(
                            id,
                            fleetId,
                            number,
                            dName,
                            dPhone,
                            fare,
                            notes,
                          )
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
                          currentDestination = AppDestination.BOOK_VEHICLE
                        },
                      )
                    } else {
                      AdminFleetScreen(
                        strings = strings,
                        vehiclesState = fleetVehiclesState,
                        adminsState = adminsState,
                        onSaveVehicle = { v -> viewModel.saveFleetVehicle(v) },
                        onUpdateAvailability = { id, av ->
                          viewModel.updateFleetAvailability(id, av)
                        },
                        onDeleteVehicle = { id -> viewModel.deleteFleetVehicle(id) },
                        onAddAdmin = { uid, email, name ->
                          viewModel.addAdmin(uid, email, name)
                        },
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
