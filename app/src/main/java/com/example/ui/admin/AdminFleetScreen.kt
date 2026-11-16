package com.example.ui.admin

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminUser
import com.example.data.model.FleetVehicle
import com.example.data.model.VehicleAvailability
import com.example.data.model.VehicleType
import com.example.ui.components.dialPhoneNumber
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldOnContainer
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
  adminsState: UiState<List<AdminUser>>,
  onSaveVehicle: (FleetVehicle) -> Unit,
  onUpdateAvailability: (vehicleId: String, availability: String) -> Unit,
  onDeleteVehicle: (vehicleId: String) -> Unit,
  onAddAdmin: (uid: String, email: String, name: String) -> Unit,
) {
  val context = LocalContext.current
  var showAddVehicleDialog by remember { mutableStateOf(false) }
  var showAddAdminDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 820.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.fleetManagementTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = NavyDeep,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
          )
          Button(
            onClick = { showAddVehicleDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_fleet_vehicle_btn"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.addVehicleBtn, fontWeight = FontWeight.Bold)
          }
        }
      }

      when (vehiclesState) {
        is UiState.Loading -> {
          item {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = EmeraldPrimary)
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(text = vehiclesState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          val vehicles = vehiclesState.data
          if (vehicles.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                  Text(
                    text = strings.noFleetVehicles,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(vehicles, key = { it.id }) { vehicle ->
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

      // Admin Team Management Section
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = NavyDeep)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.adminTeamSection,
              style = MaterialTheme.typography.titleMedium,
              color = NavyDeep,
              fontWeight = FontWeight.Bold,
            )
          }
          OutlinedButton(
            onClick = { showAddAdminDialog = true },
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.addAdminBtn)
          }
        }
      }

      if (adminsState is UiState.Success) {
        items(adminsState.data, key = { it.id }) { admin ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = admin.name,
                style = MaterialTheme.typography.titleSmall,
                color = NavyDeep,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${admin.email} • UID: ${admin.uid}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
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

@Composable
private fun FleetVehicleCard(
  strings: AppStrings,
  vehicle: FleetVehicle,
  onCallDriver: () -> Unit,
  onChangeAvailability: (String) -> Unit,
  onDelete: () -> Unit,
) {
  val availabilityEnum = VehicleAvailability.fromLabel(vehicle.availability)
  val badgeBg =
    when (availabilityEnum) {
      VehicleAvailability.AVAILABLE -> EmeraldLightContainer
      VehicleAvailability.ON_TRIP -> AmberContainer
      VehicleAvailability.MAINTENANCE -> ErrorContainerLight
    }
  val badgeFg =
    when (availabilityEnum) {
      VehicleAvailability.AVAILABLE -> EmeraldOnContainer
      VehicleAvailability.ON_TRIP -> AmberOnContainer
      VehicleAvailability.MAINTENANCE -> ErrorRed
    }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = NavyDeep)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = vehicle.vehicleNumber,
              style = MaterialTheme.typography.titleMedium,
              color = NavyDeep,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = vehicle.vehicleType,
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldPrimary,
            )
          }
        }
        Surface(color = badgeBg, shape = RoundedCornerShape(16.dp)) {
          Text(
            text = if (strings.isHindi) availabilityEnum.labelHi else availabilityEnum.label,
            color = badgeFg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "${strings.driverLabel}: ${vehicle.driverName} (${vehicle.driverPhone})",
        style = MaterialTheme.typography.bodyMedium,
      )
      if (vehicle.currentCity.isNotBlank()) {
        Text(
          text = "${strings.currentCityInput}: ${vehicle.currentCity}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          VehicleAvailability.entries.forEach { av ->
            FilterChip(
              selected = vehicle.availability == av.label,
              onClick = { onChangeAvailability(av.label) },
              label = { Text(if (strings.isHindi) av.labelHi else av.label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = NavyDeep,
                  selectedLabelColor = PureWhite,
                ),
            )
          }
        }
        IconButton(onClick = onCallDriver) {
          Icon(Icons.Filled.Call, contentDescription = strings.callDriver, tint = EmeraldPrimary)
        }
        IconButton(onClick = onDelete) {
          Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ErrorRed)
        }
      }
    }
  }
}

@Composable
private fun AddVehicleDialog(
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSave: (FleetVehicle) -> Unit,
) {
  var number by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf(VehicleType.WHEELER_10) }
  var driverName by remember { mutableStateOf("") }
  var driverPhone by remember { mutableStateOf("") }
  var city by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addVehicleBtn, fontWeight = FontWeight.Bold, color = NavyDeep) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = number,
          onValueChange = { number = it.uppercase() },
          label = { Text(strings.vehicleNumberInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          VehicleType.entries.forEach { vt ->
            FilterChip(
              selected = selectedType == vt,
              onClick = { selectedType = vt },
              label = { Text(vt.label) },
            )
          }
        }
        OutlinedTextField(
          value = driverName,
          onValueChange = { driverName = it },
          label = { Text(strings.driverNameInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = driverPhone,
          onValueChange = { driverPhone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
          label = { Text(strings.driverPhoneInput) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = city,
          onValueChange = { city = it },
          label = { Text(strings.currentCityInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            FleetVehicle(
              vehicleNumber = number.trim(),
              vehicleType = selectedType.label,
              driverName = driverName.trim(),
              driverPhone = driverPhone.trim(),
              availability = VehicleAvailability.AVAILABLE.label,
              currentCity = city.trim(),
            )
          )
        },
        enabled =
          number.trim().length >= 4 &&
            driverName.trim().length >= 2 &&
            driverPhone.filter { it.isDigit() }.length >= 10,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
      ) {
        Text(strings.saveVehicleBtn)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}

@Composable
private fun AddAdminDialog(
  strings: AppStrings,
  onDismiss: () -> Unit,
  onAdd: (uid: String, email: String, name: String) -> Unit,
) {
  var uid by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addAdminTitle, fontWeight = FontWeight.Bold, color = NavyDeep) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = uid,
          onValueChange = { uid = it },
          label = { Text(strings.adminUidInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(strings.adminEmailInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.adminNameInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onAdd(uid.trim(), email.trim(), name.trim()) },
        enabled = uid.trim().isNotEmpty() && email.trim().contains("@") && name.trim().length >= 2,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
      ) {
        Text(strings.addAdminBtn)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}
