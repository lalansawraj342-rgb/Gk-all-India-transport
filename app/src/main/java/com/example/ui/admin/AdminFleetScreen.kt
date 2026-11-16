package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminUser
import com.example.data.model.Booking
import com.example.data.model.Driver
import com.example.data.model.FleetVehicle
import com.example.data.model.PaymentRecord
import com.example.data.model.UserProfile
import com.example.ui.components.dialPhoneNumber
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.UiState

@Composable
fun AdminFleetScreen(
  strings: AppStrings,
  vehiclesState: UiState<List<FleetVehicle>>,
  driversState: UiState<List<Driver>>,
  customersState: UiState<List<UserProfile>>,
  paymentsState: UiState<List<PaymentRecord>>,
  bookingsState: UiState<List<Booking>>,
  adminsState: UiState<List<AdminUser>>,
  onSaveVehicle: (FleetVehicle) -> Unit,
  onUpdateAvailability: (vehicleId: String, availability: String) -> Unit,
  onDeleteVehicle: (vehicleId: String) -> Unit,
  onSaveDriver: (Driver) -> Unit,
  onUpdateDriverStatus: (driverId: String, availability: String, tripStatus: String) -> Unit,
  onDeleteDriver: (driverId: String) -> Unit,
  onVerifyPayment: (PaymentRecord, Boolean, Booking?) -> Unit,
  onAddAdmin: (uid: String, email: String, name: String) -> Unit,
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }
  var showAddVehicleDialog by remember { mutableStateOf(false) }
  var showAddDriverDialog by remember { mutableStateOf(false) }
  var showAddAdminDialog by remember { mutableStateOf(false) }

  val bookings = (bookingsState as? UiState.Success)?.data ?: emptyList()

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.widthIn(max = 820.dp).fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.fleetManagementTitle,
          style = MaterialTheme.typography.titleLarge,
          color = NavyDeep,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        when (selectedTab) {
          0 -> {
            Button(
              onClick = { showAddVehicleDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("add_fleet_vehicle_btn"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.addVehicleBtn, fontWeight = FontWeight.Bold)
            }
          }
          1 -> {
            Button(
              onClick = { showAddDriverDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("add_driver_btn"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.addDriverBtn, fontWeight = FontWeight.Bold)
            }
          }
          4 -> {
            Button(
              onClick = { showAddAdminDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("add_admin_btn"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.addAdminBtn, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      val tabs =
        listOf(
          strings.tabVehicles,
          strings.tabDrivers,
          strings.tabCustomers,
          strings.tabPayments,
          strings.tabAdmins,
        )
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = PureWhite,
        contentColor = EmeraldPrimary,
        edgePadding = 12.dp,
      ) {
        tabs.forEachIndexed { idx, title ->
          Tab(
            selected = selectedTab == idx,
            onClick = { selectedTab = idx },
            text = {
              Text(
                text = title,
                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
              )
            },
          )
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        when (selectedTab) {
          0 -> {
            when (vehiclesState) {
              is UiState.Loading ->
                item { LoadingBox() }
              is UiState.Error ->
                item { Text(vehiclesState.message, color = MaterialTheme.colorScheme.error) }
              is UiState.Success -> {
                if (vehiclesState.data.isEmpty()) {
                  item { EmptyCard(strings.noFleetVehicles) }
                } else {
                  items(vehiclesState.data, key = { it.id }) { vehicle ->
                    FleetVehicleCard(
                      strings = strings,
                      vehicle = vehicle,
                      onCallDriver = { dialPhoneNumber(context, vehicle.driverPhone) },
                      onChangeAvailability = { next -> onUpdateAvailability(vehicle.id, next) },
                      onDelete = { onDeleteVehicle(vehicle.id) },
                    )
                  }
                }
              }
            }
          }
          1 -> {
            when (driversState) {
              is UiState.Loading ->
                item { LoadingBox() }
              is UiState.Error ->
                item { Text(driversState.message, color = MaterialTheme.colorScheme.error) }
              is UiState.Success -> {
                if (driversState.data.isEmpty()) {
                  item { EmptyCard(strings.noDriversYet) }
                } else {
                  items(driversState.data, key = { it.id }) { driver ->
                    DriverCard(
                      strings = strings,
                      driver = driver,
                      onCallDriver = { dialPhoneNumber(context, driver.mobileNumber) },
                      onUpdateStatus = { av, ts -> onUpdateDriverStatus(driver.id, av, ts) },
                      onDelete = { onDeleteDriver(driver.id) },
                    )
                  }
                }
              }
            }
          }
          2 -> {
            when (customersState) {
              is UiState.Loading ->
                item { LoadingBox() }
              is UiState.Error ->
                item { Text(customersState.message, color = MaterialTheme.colorScheme.error) }
              is UiState.Success -> {
                if (customersState.data.isEmpty()) {
                  item { EmptyCard(strings.noCustomersYet) }
                } else {
                  items(customersState.data, key = { it.userId.ifBlank { it.id } }) { customer ->
                    val count = bookings.count { it.userId == customer.userId }
                    CustomerRecordCard(
                      strings = strings,
                      customer = customer,
                      bookingCount = count,
                      onCallCustomer = { dialPhoneNumber(context, customer.mobileNumber) },
                    )
                  }
                }
              }
            }
          }
          3 -> {
            when (paymentsState) {
              is UiState.Loading ->
                item { LoadingBox() }
              is UiState.Error ->
                item { Text(paymentsState.message, color = MaterialTheme.colorScheme.error) }
              is UiState.Success -> {
                if (paymentsState.data.isEmpty()) {
                  item { EmptyCard(strings.noPaymentsYet) }
                } else {
                  items(paymentsState.data, key = { it.id }) { payment ->
                    val matchingBooking = bookings.firstOrNull { it.id == payment.bookingId }
                    AdminPaymentRecordCard(
                      strings = strings,
                      payment = payment,
                      onApprove = { onVerifyPayment(payment, true, matchingBooking) },
                      onReject = { onVerifyPayment(payment, false, matchingBooking) },
                    )
                  }
                }
              }
            }
          }
          else -> {
            when (adminsState) {
              is UiState.Loading ->
                item { LoadingBox() }
              is UiState.Error ->
                item { Text(adminsState.message, color = MaterialTheme.colorScheme.error) }
              is UiState.Success -> {
                items(adminsState.data, key = { it.id }) { admin ->
                  AdminMemberCard(admin = admin)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddVehicleDialog) {
    AddVehicleDialog(
      strings = strings,
      onDismiss = { showAddVehicleDialog = false },
      onSave = { v ->
        onSaveVehicle(v)
        showAddVehicleDialog = false
      },
    )
  }

  if (showAddDriverDialog) {
    AddDriverDialog(
      strings = strings,
      onDismiss = { showAddDriverDialog = false },
      onSave = { d ->
        onSaveDriver(d)
        showAddDriverDialog = false
      },
    )
  }

  if (showAddAdminDialog) {
    AddAdminDialog(
      strings = strings,
      onDismiss = { showAddAdminDialog = false },
      onAdd = { uid, email, name ->
        onAddAdmin(uid, email, name)
        showAddAdminDialog = false
      },
    )
  }
}

@Composable
private fun LoadingBox() {
  Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = EmeraldPrimary)
  }
}

@Composable
private fun EmptyCard(text: String) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun AdminAccessRestrictedScreen(
  strings: AppStrings,
  currentUid: String,
  currentEmail: String,
  onReturnCustomer: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = ErrorContainerLight,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.size(64.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Filled.Lock,
              contentDescription = null,
              tint = ErrorRed,
              modifier = Modifier.size(34.dp),
            )
          }
        }
        Text(
          text = strings.adminAccessRestrictedTitle,
          style = MaterialTheme.typography.headlineSmall,
          color = NavyDeep,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = strings.adminAccessRestrictedBody,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "${strings.yourAccountUid}: $currentUid",
              style = MaterialTheme.typography.labelMedium,
              color = NavyDeep,
            )
            if (currentEmail.isNotBlank()) {
              Text(
                text = currentEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        Button(
          onClick = onReturnCustomer,
          modifier = Modifier.fillMaxWidth().height(50.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text(strings.switchToCustomer, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
