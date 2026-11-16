package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.VehicleType
import com.example.ui.components.BookingStatusBadge
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
  onCancelBooking: (String) -> Unit,
  onBookNewClick: () -> Unit,
) {
  var cancelTarget by remember { mutableStateOf<Booking?>(null) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
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
        val bookings = bookingsState.data
        if (bookings.isEmpty()) {
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
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              text = strings.noBookingsYet,
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
            Spacer(modifier = Modifier.height(22.dp))
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
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            item {
              Text(
                text = strings.myBookingsTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = NavyDeep,
                fontWeight = FontWeight.Bold,
              )
            }
            items(bookings, key = { it.id }) { booking ->
              CustomerBookingCard(
                strings = strings,
                booking = booking,
                onRequestCancel = { cancelTarget = booking },
              )
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
}

@Composable
private fun CustomerBookingCard(
  strings: AppStrings,
  booking: Booking,
  onRequestCancel: () -> Unit,
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

      Spacer(modifier = Modifier.height(12.dp))

      // Route
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Filled.MyLocation,
          contentDescription = null,
          tint = EmeraldPrimary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${strings.pickupShort}: ${booking.pickupLocation}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = NavyDeep,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Filled.LocationOn,
          contentDescription = null,
          tint = NavyDeep,
          modifier = Modifier.size(18.dp),
        )
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
          Icon(
            Icons.Filled.LocalShipping,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = vehicleDisplay,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
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
        Icon(
          Icons.Filled.Inventory2,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${booking.goodsDescription} (${booking.goodsWeight})",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (booking.specialInstructions.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${strings.instructionsShort}: ${booking.specialInstructions}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (booking.estimatedFare.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(color = EmeraldLightContainer, shape = RoundedCornerShape(10.dp)) {
          Text(
            text = "${strings.estimatedFareLabel}: ${booking.estimatedFare}",
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldOnContainer,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }

      if (booking.adminNotes.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "${strings.adminRemarksLabel}: ${booking.adminNotes}",
          style = MaterialTheme.typography.bodySmall,
          color = NavyDeep,
          fontWeight = FontWeight.Medium,
        )
      }

      if (booking.assignedVehicleNumber.isNotBlank() || booking.assignedDriverName.isNotBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          color = NavyContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = NavyDeep,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = strings.assignedVehicleHeader,
                style = MaterialTheme.typography.labelLarge,
                color = NavyOnContainer,
                fontWeight = FontWeight.Bold,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
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
              Spacer(modifier = Modifier.height(8.dp))
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

      if (status == BookingStatus.NEW_BOOKING || status == BookingStatus.PENDING) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.awaitingAdminApproval,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedButton(
            onClick = onRequestCancel,
            border = BorderStroke(1.dp, ErrorRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.cancelBooking)
          }
        }
      }
    }
  }
}
