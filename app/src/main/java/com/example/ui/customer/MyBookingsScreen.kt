package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRecordStatus
import com.example.data.model.VehicleType
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.BookingStatusTimeline
import com.example.ui.components.GoodsPhotoStrip
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.dialPhoneNumber
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyOnContainer
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.UiState

@Composable
fun MyBookingsScreen(
  strings: AppStrings,
  bookingsState: UiState<List<Booking>>,
  paymentsState: UiState<List<PaymentRecord>>,
  initialTabIndex: Int = 0,
  initialDetailBooking: Booking? = null,
  onClearInitialDetail: () -> Unit = {},
  onCancelBooking: (String) -> Unit,
  onSubmitPayment: (Booking, Double, String, String, String) -> Unit,
  onBookNewClick: () -> Unit,
  onSupportClick: () -> Unit,
) {
  var selectedTab by remember(initialTabIndex) { mutableIntStateOf(initialTabIndex) }
  var cancelTarget by remember { mutableStateOf<Booking?>(null) }
  var detailTarget by remember { mutableStateOf<Booking?>(initialDetailBooking) }
  var paymentTarget by remember { mutableStateOf<Booking?>(null) }

  LaunchedEffect(initialDetailBooking) {
    if (initialDetailBooking != null) {
      detailTarget = initialDetailBooking
      onClearInitialDetail()
    }
  }

  Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = PureWhite,
      contentColor = EmeraldPrimary,
      modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text(strings.activeBookingsTab, fontWeight = FontWeight.Bold) },
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text(strings.previousBookingsTab, fontWeight = FontWeight.Bold) },
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text(strings.paymentHistoryTab, fontWeight = FontWeight.Bold) },
      )
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      if (selectedTab == 2) {
        CustomerPaymentHistoryList(strings = strings, paymentsState = paymentsState)
      } else {
        when (bookingsState) {
          is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = EmeraldPrimary)
            }
          }
          is UiState.Error -> {
            Box(
              modifier = Modifier.fillMaxSize().padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = bookingsState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
              )
            }
          }
          is UiState.Success -> {
            val filtered =
              bookingsState.data.filter { b ->
                val st = BookingStatus.fromLabel(b.status)
                if (selectedTab == 0) st.isActiveTrip() else st.isPreviousTrip()
              }

            if (filtered.isEmpty()) {
              Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Surface(
                  color = EmeraldLightContainer,
                  shape = RoundedCornerShape(24.dp),
                  modifier = Modifier.size(84.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Filled.LocalShipping,
                      contentDescription = null,
                      tint = EmeraldPrimary,
                      modifier = Modifier.size(44.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                  text = if (selectedTab == 0) strings.noBookingsYet else strings.noPreviousBookings,
                  style = MaterialTheme.typography.headlineSmall,
                  color = NavyDeep,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = strings.noBookingsSubtitle,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                  onClick = onBookNewClick,
                  modifier = Modifier.height(52.dp).testTag("empty_book_vehicle_btn"),
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                ) {
                  Text(strings.bookFirstVehicle, fontWeight = FontWeight.Bold)
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                items(filtered, key = { it.id }) { booking ->
                  CustomerBookingCard(
                    strings = strings,
                    booking = booking,
                    onViewDetails = { detailTarget = booking },
                    onPayClick = { paymentTarget = booking },
                    onRequestCancel = { cancelTarget = booking },
                    onSupportClick = onSupportClick,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  cancelTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { cancelTarget = null },
      title = { Text(strings.cancelConfirmTitle, fontWeight = FontWeight.Bold) },
      text = { Text(strings.cancelConfirmBody) },
      confirmButton = {
        Button(
          onClick = {
            onCancelBooking(target.id)
            cancelTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
        ) {
          Text(strings.yesCancel)
        }
      },
      dismissButton = {
        TextButton(onClick = { cancelTarget = null }) { Text(strings.keepBooking) }
      },
    )
  }

  detailTarget?.let { target ->
    val liveBooking =
      (bookingsState as? UiState.Success)?.data?.firstOrNull { it.id == target.id } ?: target
    BookingDetailTrackingDialog(
      strings = strings,
      booking = liveBooking,
      onDismiss = { detailTarget = null },
      onPayClick = {
        detailTarget = null
        paymentTarget = liveBooking
      },
    )
  }

  paymentTarget?.let { target ->
    SubmitPaymentDialog(
      strings = strings,
      booking = target,
      onDismiss = { paymentTarget = null },
      onSubmit = { amount, method, ref, notes ->
        onSubmitPayment(target, amount, method, ref, notes)
        paymentTarget = null
      },
    )
  }
}

@Composable
private fun CustomerBookingCard(
  strings: AppStrings,
  booking: Booking,
  onViewDetails: () -> Unit,
  onPayClick: () -> Unit,
  onRequestCancel: () -> Unit,
  onSupportClick: () -> Unit,
) {
  val context = LocalContext.current
  val status = BookingStatus.fromLabel(booking.status)
  val vehicleEnum = VehicleType.fromLabel(booking.vehicleType)
  val vehicleDisplay = if (strings.isHindi) vehicleEnum.labelHi else vehicleEnum.label

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag("customer_booking_card_${booking.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = booking.bookingCode,
          style = MaterialTheme.typography.labelLarge,
          color = NavyDeep,
          fontWeight = FontWeight.ExtraBold,
        )
        BookingStatusBadge(statusLabel = booking.status, isHindi = strings.isHindi)
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${strings.pickupShort}: ${booking.pickupLocation}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = NavyDeep,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${strings.dropShort}: ${booking.deliveryLocation}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = NavyDeep,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = vehicleDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${booking.bookingDateLabel} • ${booking.bookingTimeLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${booking.goodsDescription} (${booking.goodsWeight})",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (booking.goodsPhotos.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        GoodsPhotoStrip(photosBase64 = booking.goodsPhotos)
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (booking.estimatedFare.isNotBlank()) {
          Surface(color = EmeraldLightContainer, shape = RoundedCornerShape(10.dp)) {
            Text(
              text = "${strings.estimatedFareLabel}: ${booking.estimatedFare}",
              style = MaterialTheme.typography.labelLarge,
              color = EmeraldOnContainer,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        } else {
          Spacer(modifier = Modifier.width(1.dp))
        }
        PaymentStatusBadge(paymentStatusLabel = booking.paymentStatus, isHindi = strings.isHindi)
      }

      if (booking.assignedVehicleNumber.isNotBlank() || booking.assignedDriverName.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(color = NavyContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = strings.assignedVehicleHeader,
                style = MaterialTheme.typography.labelLarge,
                color = NavyOnContainer,
                fontWeight = FontWeight.Bold,
              )
            }
            if (booking.assignedVehicleNumber.isNotBlank()) {
              Text(
                text = "${strings.vehicleNoLabel}: ${booking.assignedVehicleNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = NavyDeep,
              )
            }
            if (booking.assignedDriverName.isNotBlank()) {
              Text(
                text = "${strings.driverLabel}: ${booking.assignedDriverName} (${booking.assignedDriverPhone})",
                style = MaterialTheme.typography.bodySmall,
                color = NavyOnContainer,
              )
            }
            if (booking.assignedDriverPhone.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Button(
                onClick = { dialPhoneNumber(context, booking.assignedDriverPhone) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.callDriver)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onViewDetails,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(Icons.Filled.LocationSearching, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.viewDetailsTrackBtn)
        }
        if (status != BookingStatus.CANCELLED) {
          OutlinedButton(
            onClick = onPayClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.payNowBtn, color = EmeraldPrimary)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onSupportClick) {
          Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(strings.callSupportBtn)
        }
        if (status.canCustomerCancel()) {
          OutlinedButton(
            onClick = onRequestCancel,
            border = BorderStroke(1.dp, ErrorRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.cancelBooking)
          }
        }
      }
    }
  }
}

@Composable
private fun BookingDetailTrackingDialog(
  strings: AppStrings,
  booking: Booking,
  onDismiss: () -> Unit,
  onPayClick: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "${strings.bookingDetailsTitle} • ${booking.bookingCode}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = NavyDeep,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "${strings.pickupShort}: ${booking.pickupLocation}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "${strings.dropShort}: ${booking.deliveryLocation}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "${strings.vehicleShort}: ${booking.vehicleType} • ${booking.goodsWeight}",
          style = MaterialTheme.typography.bodySmall,
        )
        if (booking.estimatedFare.isNotBlank()) {
          Text(
            text = "${strings.estimatedFareLabel}: ${booking.estimatedFare} (${strings.paidAmountLabel}: ₹${booking.paidAmount.toLong()})",
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldPrimary,
          )
        }
        if (booking.adminNotes.isNotBlank()) {
          Text(
            text = "${strings.adminRemarksLabel}: ${booking.adminNotes}",
            style = MaterialTheme.typography.bodySmall,
            color = NavyDeep,
          )
        }
        if (booking.goodsPhotos.isNotEmpty()) {
          GoodsPhotoStrip(photosBase64 = booking.goodsPhotos)
        }
        HorizontalDivider()
        Text(
          text = strings.liveTrackingTimeline,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = NavyDeep,
        )
        BookingStatusTimeline(currentStatusLabel = booking.status, isHindi = strings.isHindi)
      }
    },
    confirmButton = {
      Button(
        onClick = onPayClick,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
      ) {
        Text(strings.payNowBtn)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.closeBtn) }
    },
  )
}

@Composable
private fun SubmitPaymentDialog(
  strings: AppStrings,
  booking: Booking,
  onDismiss: () -> Unit,
  onSubmit: (amount: Double, method: String, ref: String, notes: String) -> Unit,
) {
  var amountText by remember {
    mutableStateOf(
      if (booking.totalFareAmount > 0) booking.totalFareAmount.toLong().toString() else ""
    )
  }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
  var referenceId by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "${strings.submitPaymentTitle} (${booking.bookingCode})",
        fontWeight = FontWeight.Bold,
        color = NavyDeep,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
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
                  selectedContainerColor = EmeraldPrimary,
                  selectedLabelColor = PureWhite,
                ),
            )
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(strings.paymentAmountInput) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
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
      val amount = amountText.toDoubleOrNull() ?: 0.0
      Button(
        onClick = { onSubmit(amount, selectedMethod.label, referenceId, notes) },
        enabled = amount > 0.0,
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
private fun CustomerPaymentHistoryList(
  strings: AppStrings,
  paymentsState: UiState<List<PaymentRecord>>,
) {
  when (paymentsState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = EmeraldPrimary)
      }
    }
    is UiState.Error -> {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = paymentsState.message, color = MaterialTheme.colorScheme.error)
      }
    }
    is UiState.Success -> {
      val payments = paymentsState.data
      if (payments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(
            text = strings.noPaymentsYet,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(payments, key = { it.id }) { pay ->
            val pStatus = PaymentRecordStatus.fromLabel(pay.status)
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = PureWhite),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${pay.bookingCode} • ${pay.method}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep,
                  )
                  if (pay.referenceId.isNotBlank()) {
                    Text(
                      text = "Ref: ${pay.referenceId}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = if (strings.isHindi) pStatus.labelHi else pStatus.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
                Text(
                  text = "₹${pay.amount.toLong()}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = NavyDeep,
                )
              }
            }
          }
        }
      }
    }
  }
}
