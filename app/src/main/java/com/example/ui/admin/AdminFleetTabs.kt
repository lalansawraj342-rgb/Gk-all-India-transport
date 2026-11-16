package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminUser
import com.example.data.model.Driver
import com.example.data.model.DriverAvailability
import com.example.data.model.DriverTripStatus
import com.example.data.model.FleetVehicle
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.UserProfile
import com.example.data.model.VehicleAvailability
import com.example.data.model.VehicleType
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorOnContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyOnContainer
import com.example.ui.theme.PureWhite

@Composable
fun FleetVehicleCard(
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
            val vtEnum = VehicleType.fromLabel(vehicle.vehicleType)
            Text(
              text = if (strings.isHindi) vtEnum.labelHi else vtEnum.label,
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
fun DriverCard(
  strings: AppStrings,
  driver: Driver,
  onCallDriver: () -> Unit,
  onUpdateStatus: (availability: String, tripStatus: String) -> Unit,
  onDelete: () -> Unit,
) {
  val avEnum = DriverAvailability.fromLabel(driver.availability)
  val tripEnum = DriverTripStatus.fromLabel(driver.tripStatus)
  val badgeBg =
    when (avEnum) {
      DriverAvailability.AVAILABLE -> EmeraldLightContainer
      DriverAvailability.ON_TRIP -> AmberContainer
      DriverAvailability.OFF_DUTY -> ErrorContainerLight
    }
  val badgeFg =
    when (avEnum) {
      DriverAvailability.AVAILABLE -> EmeraldOnContainer
      DriverAvailability.ON_TRIP -> AmberOnContainer
      DriverAvailability.OFF_DUTY -> ErrorRed
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Filled.Badge, contentDescription = null, tint = NavyDeep)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = driver.name,
              style = MaterialTheme.typography.titleMedium,
              color = NavyDeep,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = driver.mobileNumber,
              style = MaterialTheme.typography.bodySmall,
              color = EmeraldPrimary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        Surface(color = badgeBg, shape = RoundedCornerShape(16.dp)) {
          Text(
            text = if (strings.isHindi) avEnum.labelHi else avEnum.label,
            color = badgeFg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "DL: ${driver.licenseNumber}",
        style = MaterialTheme.typography.bodySmall,
        color = NavyDeep,
      )
      if (driver.assignedVehicleNumber.isNotBlank()) {
        Text(
          text = "${strings.vehicleNoLabel}: ${driver.assignedVehicleNumber}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = "${strings.tripStatusLabel}: ${if (strings.isHindi) tripEnum.labelHi else tripEnum.label}",
        style = MaterialTheme.typography.labelMedium,
        color = NavyDeep,
        fontWeight = FontWeight.SemiBold,
      )

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        DriverAvailability.entries.forEach { av ->
          FilterChip(
            selected = driver.availability == av.label,
            onClick = { onUpdateStatus(av.label, driver.tripStatus) },
            label = { Text(if (strings.isHindi) av.labelHi else av.label) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = EmeraldPrimary,
                selectedLabelColor = PureWhite,
              ),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          DriverTripStatus.entries.forEach { ts ->
            FilterChip(
              selected = driver.tripStatus == ts.label,
              onClick = { onUpdateStatus(driver.availability, ts.label) },
              label = { Text(if (strings.isHindi) ts.labelHi else ts.label) },
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
fun CustomerRecordCard(
  strings: AppStrings,
  customer: UserProfile,
  bookingCount: Int,
  onCallCustomer: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Filled.Person, contentDescription = null, tint = NavyDeep)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = customer.name,
              style = MaterialTheme.typography.titleMedium,
              color = NavyDeep,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = customer.mobileNumber,
              style = MaterialTheme.typography.bodyMedium,
              color = EmeraldPrimary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        Surface(color = NavyContainer, shape = RoundedCornerShape(12.dp)) {
          Text(
            text = "$bookingCount ${strings.tabMyBookings}",
            color = NavyOnContainer,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      if (customer.email.isNotBlank()) {
        Text(
          text = customer.email,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (customer.city.isNotBlank() || customer.address.isNotBlank()) {
        Text(
          text = listOf(customer.address, customer.city).filter { it.isNotBlank() }.joinToString(", "),
          style = MaterialTheme.typography.bodySmall,
          color = NavyDeep,
        )
      }
      if (customer.alternatePhone.isNotBlank()) {
        Text(
          text = "${strings.altPhoneLabel}: ${customer.alternatePhone}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
          onClick = onCallCustomer,
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.callCustomer)
        }
      }
    }
  }
}

@Composable
fun AdminPaymentRecordCard(
  strings: AppStrings,
  payment: PaymentRecord,
  onApprove: () -> Unit,
  onReject: () -> Unit,
) {
  val statusEnum = PaymentRecordStatus.fromLabel(payment.status)
  val methodEnum = PaymentMethod.fromLabel(payment.method)
  val (bg, fg) =
    when (statusEnum) {
      PaymentRecordStatus.PENDING_VERIFICATION -> AmberContainer to AmberOnContainer
      PaymentRecordStatus.PAID -> EmeraldLightContainer to EmeraldOnContainer
      PaymentRecordStatus.REJECTED -> ErrorContainerLight to ErrorOnContainerLight
    }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Payments, contentDescription = null, tint = EmeraldPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "₹${payment.amount.toLong()} • ${if (strings.isHindi) methodEnum.labelHi else methodEnum.label}",
              style = MaterialTheme.typography.titleMedium,
              color = NavyDeep,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "${payment.bookingCode} • ${payment.customerName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
          Text(
            text = if (strings.isHindi) statusEnum.labelHi else statusEnum.label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
      if (payment.referenceId.isNotBlank()) {
        Text(
          text = "Ref: ${payment.referenceId}",
          style = MaterialTheme.typography.bodySmall,
          color = NavyDeep,
        )
      }
      if (payment.notes.isNotBlank()) {
        Text(
          text = payment.notes,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (statusEnum == PaymentRecordStatus.PENDING_VERIFICATION) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onApprove,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.verifyApproveBtn)
          }
          OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, ErrorRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.rejectPaymentBtn)
          }
        }
      }
    }
  }
}

@Composable
fun AdminMemberCard(admin: AdminUser) {
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

@Composable
fun AddVehicleDialog(
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSave: (FleetVehicle) -> Unit,
) {
  var number by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf(VehicleType.TATA_ACE) }
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
              label = { Text(if (strings.isHindi) vt.labelHi else vt.label) },
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
fun AddDriverDialog(
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSave: (Driver) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var mobile by remember { mutableStateOf("") }
  var license by remember { mutableStateOf("") }
  var assignedVehicle by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addDriverBtn, fontWeight = FontWeight.Bold, color = NavyDeep) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.driverNameInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = mobile,
          onValueChange = { mobile = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
          label = { Text(strings.driverPhoneInput) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = license,
          onValueChange = { license = it.uppercase() },
          label = { Text(strings.driverLicenseInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = assignedVehicle,
          onValueChange = { assignedVehicle = it.uppercase() },
          label = { Text(strings.assignedVehicleOptInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            Driver(
              name = name.trim(),
              mobileNumber = mobile.trim(),
              licenseNumber = license.trim(),
              assignedVehicleNumber = assignedVehicle.trim(),
              availability = DriverAvailability.AVAILABLE.label,
              tripStatus = DriverTripStatus.IDLE.label,
            )
          )
        },
        enabled =
          name.trim().length >= 2 &&
            mobile.filter { it.isDigit() }.length >= 10 &&
            license.trim().length >= 4,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
      ) {
        Text(strings.saveDriverBtn)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}

@Composable
fun AddAdminDialog(
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
