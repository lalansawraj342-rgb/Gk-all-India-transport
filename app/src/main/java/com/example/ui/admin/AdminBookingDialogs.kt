package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.Driver
import com.example.data.model.FleetVehicle
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PureWhite

@Composable
fun AssignVehicleDriverDialog(
  strings: AppStrings,
  booking: Booking,
  availableVehicles: List<FleetVehicle>,
  availableDrivers: List<Driver>,
  onDismiss: () -> Unit,
  onConfirmAssign:
    (
      fleetId: String?,
      vehicleNumber: String,
      driverId: String?,
      driverName: String,
      driverPhone: String,
      fareLabel: String,
      totalFareAmount: Double,
      notes: String,
    ) -> Unit,
) {
  var selectedFleetId by remember {
    mutableStateOf(booking.assignedVehicleId.takeIf { it.isNotBlank() })
  }
  var selectedDriverId by remember {
    mutableStateOf(booking.assignedDriverId.takeIf { it.isNotBlank() })
  }
  var vehicleNumber by remember { mutableStateOf(booking.assignedVehicleNumber) }
  var driverName by remember { mutableStateOf(booking.assignedDriverName) }
  var driverPhone by remember { mutableStateOf(booking.assignedDriverPhone) }
  var fareLabel by remember { mutableStateOf(booking.estimatedFare) }
  var totalFareInput by remember {
    mutableStateOf(if (booking.totalFareAmount > 0) booking.totalFareAmount.toLong().toString() else "")
  }
  var notes by remember { mutableStateOf(booking.adminNotes) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.assignVehicleTitle, fontWeight = FontWeight.Bold, color = NavyDeep) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (availableVehicles.isNotEmpty()) {
          Text(
            text = strings.selectFromFleet,
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldPrimary,
          )
          availableVehicles.forEach { v ->
            val isPicked = selectedFleetId == v.id
            Surface(
              color =
                if (isPicked) EmeraldLightContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(10.dp),
              border = if (isPicked) BorderStroke(1.5.dp, EmeraldPrimary) else null,
              modifier =
                Modifier.fillMaxWidth().clickable {
                  selectedFleetId = v.id
                  vehicleNumber = v.vehicleNumber
                  if (driverName.isBlank() && v.driverName.isNotBlank()) {
                    driverName = v.driverName
                    driverPhone = v.driverPhone
                  }
                },
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(
                  selected = isPicked,
                  onClick = {
                    selectedFleetId = v.id
                    vehicleNumber = v.vehicleNumber
                    if (driverName.isBlank() && v.driverName.isNotBlank()) {
                      driverName = v.driverName
                      driverPhone = v.driverPhone
                    }
                  },
                )
                Column {
                  Text(
                    text = "${v.vehicleNumber} (${v.vehicleType})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${v.driverName} • ${v.driverPhone}",
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }
        }

        if (availableDrivers.isNotEmpty()) {
          Text(
            text = strings.selectDriverLabel,
            style = MaterialTheme.typography.labelLarge,
            color = NavyDeep,
          )
          availableDrivers.forEach { d ->
            val isPicked = selectedDriverId == d.id
            Surface(
              color =
                if (isPicked) NavyContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(10.dp),
              border = if (isPicked) BorderStroke(1.5.dp, NavyDeep) else null,
              modifier =
                Modifier.fillMaxWidth().clickable {
                  selectedDriverId = d.id
                  driverName = d.name
                  driverPhone = d.mobileNumber
                  if (vehicleNumber.isBlank() && d.assignedVehicleNumber.isNotBlank()) {
                    vehicleNumber = d.assignedVehicleNumber
                  }
                },
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(
                  selected = isPicked,
                  onClick = {
                    selectedDriverId = d.id
                    driverName = d.name
                    driverPhone = d.mobileNumber
                    if (vehicleNumber.isBlank() && d.assignedVehicleNumber.isNotBlank()) {
                      vehicleNumber = d.assignedVehicleNumber
                    }
                  },
                )
                Column {
                  Text(
                    text = "${d.name} • ${d.mobileNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "DL: ${d.licenseNumber}",
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }
        }

        Text(
          text = strings.orEnterManually,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = vehicleNumber,
          onValueChange = {
            vehicleNumber = it.uppercase()
            selectedFleetId = null
          },
          label = { Text(strings.vehicleNumberInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = driverName,
          onValueChange = {
            driverName = it
            selectedDriverId = null
          },
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
          value = totalFareInput,
          onValueChange = { raw ->
            totalFareInput = raw.filter { it.isDigit() || it == '.' }
            if (fareLabel.isBlank() || fareLabel.startsWith("₹")) {
              val amt = totalFareInput.toDoubleOrNull()
              if (amt != null && amt > 0) {
                fareLabel = "₹${amt.toLong()}"
              }
            }
          },
          label = { Text(strings.totalFareNumericInput) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = fareLabel,
          onValueChange = { fareLabel = it },
          label = { Text(strings.fareQuoteInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(strings.adminNotesInput) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirmAssign(
            selectedFleetId,
            vehicleNumber.trim(),
            selectedDriverId,
            driverName.trim(),
            driverPhone.trim(),
            fareLabel.trim(),
            totalFareInput.toDoubleOrNull() ?: 0.0,
            notes.trim(),
          )
        },
        enabled = vehicleNumber.trim().length >= 4 && driverName.trim().length >= 2,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
      ) {
        Text(strings.saveAssignment)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}

@Composable
fun UpdateBookingStatusDialog(
  strings: AppStrings,
  booking: Booking,
  onDismiss: () -> Unit,
  onConfirmStatus: (newStatus: String, notes: String?) -> Unit,
) {
  var selectedStatus by remember { mutableStateOf(booking.status) }
  var notes by remember { mutableStateOf(booking.adminNotes) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.updateStatusTitle, fontWeight = FontWeight.Bold, color = NavyDeep) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        BookingStatus.entries.forEach { st ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { selectedStatus = st.label }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selectedStatus == st.label,
              onClick = { selectedStatus = st.label },
            )
            Text(
              text = if (strings.isHindi) "${st.labelHi} (${st.label})" else st.label,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (selectedStatus == st.label) FontWeight.Bold else FontWeight.Normal,
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(strings.adminNotesInput) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmStatus(selectedStatus, notes) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
      ) {
        Text(strings.updateStatusBtn)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}

@Composable
fun AdminPaymentDialog(
  strings: AppStrings,
  booking: Booking,
  onDismiss: () -> Unit,
  onRecordPayment:
    (
      amount: Double,
      method: String,
      paymentStatus: String,
      totalFareAmount: Double,
      referenceId: String,
      notes: String,
    ) -> Unit,
) {
  var totalFareText by remember {
    mutableStateOf(if (booking.totalFareAmount > 0) booking.totalFareAmount.toLong().toString() else "")
  }
  var amountText by remember { mutableStateOf("") }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var selectedStatus by remember { mutableStateOf(PaymentStatus.fromLabel(booking.paymentStatus)) }
  var referenceId by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.recordPaymentBtn, fontWeight = FontWeight.Bold, color = NavyDeep) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "${booking.bookingCode} • ${booking.customerName}",
          style = MaterialTheme.typography.labelLarge,
          color = EmeraldPrimary,
        )
        OutlinedTextField(
          value = totalFareText,
          onValueChange = { totalFareText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(strings.totalFareNumericInput) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(strings.paymentAmountInput) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          text = strings.paymentMethodLabel,
          style = MaterialTheme.typography.labelLarge,
          color = NavyDeep,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PaymentMethod.selectableMethods.forEach { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              label = { Text(if (strings.isHindi) method.labelHi else method.label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = NavyDeep,
                  selectedLabelColor = PureWhite,
                ),
            )
          }
        }
        Text(
          text = strings.paymentStatusLabel,
          style = MaterialTheme.typography.labelLarge,
          color = NavyDeep,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PaymentStatus.entries.forEach { st ->
            FilterChip(
              selected = selectedStatus == st,
              onClick = { selectedStatus = st },
              label = { Text(if (strings.isHindi) st.labelHi else st.label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EmeraldPrimary,
                  selectedLabelColor = PureWhite,
                ),
            )
          }
        }
        OutlinedTextField(
          value = referenceId,
          onValueChange = { referenceId = it },
          label = { Text(strings.paymentRefInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(strings.paymentNotesInput) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onRecordPayment(
            amountText.toDoubleOrNull() ?: 0.0,
            selectedMethod.label,
            selectedStatus.label,
            totalFareText.toDoubleOrNull() ?: booking.totalFareAmount,
            referenceId.trim(),
            notes.trim(),
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
      ) {
        Text(strings.submitPaymentBtn)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}

@Composable
fun SendCustomerNotificationDialog(
  strings: AppStrings,
  booking: Booking,
  onDismiss: () -> Unit,
  onSend: (title: String, message: String) -> Unit,
) {
  var title by remember { mutableStateOf("Update on ${booking.bookingCode}") }
  var message by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.sendNotificationTitle, fontWeight = FontWeight.Bold, color = NavyDeep) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${booking.customerName} (${booking.customerPhone})",
          style = MaterialTheme.typography.labelMedium,
          color = EmeraldPrimary,
        )
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.notificationTitleInput) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text(strings.notificationMsgInput) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSend(title.trim(), message.trim()) },
        enabled = title.trim().isNotEmpty() && message.trim().isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
      ) {
        Text(strings.sendNotificationBtn)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}
