package com.example.ui.admin

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.Driver
import com.example.data.model.DriverAvailability
import com.example.data.model.FleetVehicle
import com.example.data.model.VehicleAvailability
import com.example.data.model.VehicleType
import com.example.notifications.BookingNotificationHelper
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.GoodsPhotoStrip
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.dialPhoneNumber
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
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
fun AdminDashboardScreen(
  strings: AppStrings,
  bookingsState: UiState<List<Booking>>,
  vehiclesState: UiState<List<FleetVehicle>>,
  driversState: UiState<List<Driver>>,
  incomingAlert: Booking?,
  onDismissAlert: () -> Unit,
  onUpdateStatus: (booking: Booking, newStatus: String, notes: String?) -> Unit,
  onAssignVehicle:
    (
      booking: Booking,
      fleetVehicleId: String?,
      vehicleNumber: String,
      driverId: String?,
      driverName: String,
      driverPhone: String,
      estimatedFare: String,
      totalFareAmount: Double,
      adminNotes: String,
    ) -> Unit,
  onRecordPayment:
    (
      booking: Booking,
      amount: Double,
      method: String,
      paymentStatus: String,
      totalFareAmount: Double,
      referenceId: String,
      notes: String,
    ) -> Unit,
  onSendNotification: (booking: Booking, title: String, message: String) -> Unit,
) {
  val context = LocalContext.current
  var hasNotifPermission by remember {
    mutableStateOf(BookingNotificationHelper.hasNotificationPermission(context))
  }
  val notifPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotifPermission = granted
    }

  var searchQuery by remember { mutableStateOf("") }
  var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
  var assignTarget by remember { mutableStateOf<Booking?>(null) }
  var statusTarget by remember { mutableStateOf<Booking?>(null) }
  var paymentTarget by remember { mutableStateOf<Booking?>(null) }
  var notifyTarget by remember { mutableStateOf<Booking?>(null) }

  val vehicles = (vehiclesState as? UiState.Success)?.data ?: emptyList()
  val drivers = (driversState as? UiState.Success)?.data ?: emptyList()

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
        val allBookings = bookingsState.data
        val totalCount = allBookings.size
        val pendingCount =
          allBookings.count { BookingStatus.fromLabel(it.status) == BookingStatus.PENDING }
        val activeCount =
          allBookings.count {
            val st = BookingStatus.fromLabel(it.status)
            st != BookingStatus.PENDING && st.isActiveTrip()
          }
        val completedCount =
          allBookings.count { BookingStatus.fromLabel(it.status) == BookingStatus.DELIVERED }

        val filteredBookings =
          remember(allBookings, searchQuery, selectedStatusFilter) {
            allBookings.filter { b ->
              val matchesStatus =
                selectedStatusFilter == null ||
                  BookingStatus.fromLabel(b.status).label == selectedStatusFilter
              val q = searchQuery.trim().lowercase()
              val matchesQuery =
                q.isEmpty() ||
                  b.customerName.lowercase().contains(q) ||
                  b.customerPhone.lowercase().contains(q) ||
                  b.pickupLocation.lowercase().contains(q) ||
                  b.deliveryLocation.lowercase().contains(q) ||
                  b.bookingCode.lowercase().contains(q) ||
                  b.vehicleType.lowercase().contains(q)
              matchesStatus && matchesQuery
            }
          }

        LazyColumn(
          modifier = Modifier.widthIn(max = 820.dp).fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Text(
              text = strings.adminDashboardTitle,
              style = MaterialTheme.typography.headlineSmall,
              color = NavyDeep,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              Surface(
                color = AmberContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      Icons.Filled.NotificationsActive,
                      contentDescription = null,
                      tint = AmberAccent,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = strings.enableNotificationsBanner,
                      style = MaterialTheme.typography.bodySmall,
                      color = AmberOnContainer,
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Button(
                    onClick = {
                      notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                  ) {
                    Text(strings.allowNotificationsBtn)
                  }
                }
              }
            } else {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Filled.NotificationsActive,
                  contentDescription = null,
                  tint = EmeraldPrimary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = strings.liveNotificationsActive,
                  style = MaterialTheme.typography.labelMedium,
                  color = EmeraldPrimary,
                )
              }
            }
          }

          item {
            AnimatedVisibility(visible = incomingAlert != null) {
              incomingAlert?.let { alert ->
                Surface(
                  color = EmeraldPrimary,
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.fillMaxWidth().testTag("admin_live_alert_banner"),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      Icons.Filled.NotificationsActive,
                      contentDescription = null,
                      tint = PureWhite,
                      modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "${strings.newBookingAlertPrefix} ${alert.customerName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text =
                          "${alert.vehicleType} • ${alert.pickupLocation} → ${alert.deliveryLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.92f),
                      )
                    }
                    IconButton(onClick = onDismissAlert) {
                      Icon(
                        Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = PureWhite,
                      )
                    }
                  }
                }
              }
            }
          }

          // 4 KPI Summary Cards: Total, Pending, Active, Completed
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              KpiCard(
                label = strings.totalBookingsCount,
                count = totalCount,
                bgColor = PureWhite,
                fgColor = NavyDeep,
                modifier = Modifier.weight(1f),
              )
              KpiCard(
                label = strings.newRequestsCount,
                count = pendingCount,
                bgColor = AmberContainer,
                fgColor = AmberOnContainer,
                modifier = Modifier.weight(1f),
              )
              KpiCard(
                label = strings.activeTripsCount,
                count = activeCount,
                bgColor = NavyContainer,
                fgColor = NavyOnContainer,
                modifier = Modifier.weight(1f),
              )
              KpiCard(
                label = strings.deliveredCount,
                count = completedCount,
                bgColor = EmeraldLightContainer,
                fgColor = EmeraldOnContainer,
                modifier = Modifier.weight(1f),
              )
            }
          }

          // Search + Status Filter Chips
          item {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text(strings.searchBookingsPlaceholder) },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("admin_search_input"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = selectedStatusFilter == null,
                onClick = { selectedStatusFilter = null },
                label = { Text("${strings.filterAll} ($totalCount)") },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NavyDeep,
                    selectedLabelColor = PureWhite,
                  ),
              )
              BookingStatus.entries.forEach { st ->
                val count = allBookings.count { BookingStatus.fromLabel(it.status) == st }
                FilterChip(
                  selected = selectedStatusFilter == st.label,
                  onClick = {
                    selectedStatusFilter =
                      if (selectedStatusFilter == st.label) null else st.label
                  },
                  label = {
                    val name = if (strings.isHindi) st.labelHi else st.label
                    Text("$name ($count)")
                  },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = EmeraldPrimary,
                      selectedLabelColor = PureWhite,
                    ),
                )
              }
            }
          }

          if (filteredBookings.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Box(
                  modifier = Modifier.fillMaxWidth().padding(32.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = strings.noMatchingBookings,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(filteredBookings, key = { it.id }) { booking ->
              AdminBookingCard(
                strings = strings,
                booking = booking,
                onCallCustomer = { dialPhoneNumber(context, booking.customerPhone) },
                onCallDriver = { dialPhoneNumber(context, booking.assignedDriverPhone) },
                onAccept = {
                  onUpdateStatus(booking, BookingStatus.ACCEPTED.label, null)
                },
                onReject = {
                  onUpdateStatus(booking, BookingStatus.CANCELLED.label, null)
                },
                onAssignVehicleClick = { assignTarget = booking },
                onUpdateStatusClick = { statusTarget = booking },
                onPaymentClick = { paymentTarget = booking },
                onNotifyClick = { notifyTarget = booking },
              )
            }
          }
        }
      }
    }
  }

  assignTarget?.let { target ->
    AssignVehicleDriverDialog(
      strings = strings,
      booking = target,
      availableVehicles = vehicles.filter { it.availability == VehicleAvailability.AVAILABLE.label },
      availableDrivers = drivers.filter { it.availability == DriverAvailability.AVAILABLE.label },
      onDismiss = { assignTarget = null },
      onConfirmAssign = { fleetId, number, driverId, driverName, driverPhone, fare, totalFare, notes ->
        onAssignVehicle(
          target,
          fleetId,
          number,
          driverId,
          driverName,
          driverPhone,
          fare,
          totalFare,
          notes,
        )
        assignTarget = null
      },
    )
  }

  statusTarget?.let { target ->
    UpdateBookingStatusDialog(
      strings = strings,
      booking = target,
      onDismiss = { statusTarget = null },
      onConfirmStatus = { newStatus, notes ->
        onUpdateStatus(target, newStatus, notes)
        statusTarget = null
      },
    )
  }

  paymentTarget?.let { target ->
    AdminPaymentDialog(
      strings = strings,
      booking = target,
      onDismiss = { paymentTarget = null },
      onRecordPayment = { amount, method, paymentStatus, totalFare, ref, notes ->
        onRecordPayment(target, amount, method, paymentStatus, totalFare, ref, notes)
        paymentTarget = null
      },
    )
  }

  notifyTarget?.let { target ->
    SendCustomerNotificationDialog(
      strings = strings,
      booking = target,
      onDismiss = { notifyTarget = null },
      onSend = { title, message ->
        onSendNotification(target, title, message)
        notifyTarget = null
      },
    )
  }
}

@Composable
private fun KpiCard(
  label: String,
  count: Int,
  bgColor: Color,
  fgColor: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = bgColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.headlineSmall,
        color = fgColor,
        fontWeight = FontWeight.ExtraBold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fgColor,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun AdminBookingCard(
  strings: AppStrings,
  booking: Booking,
  onCallCustomer: () -> Unit,
  onCallDriver: () -> Unit,
  onAccept: () -> Unit,
  onReject: () -> Unit,
  onAssignVehicleClick: () -> Unit,
  onUpdateStatusClick: () -> Unit,
  onPaymentClick: () -> Unit,
  onNotifyClick: () -> Unit,
) {
  val status = BookingStatus.fromLabel(booking.status)
  val vehicleEnum = VehicleType.fromLabel(booking.vehicleType)
  val vehicleDisplay = if (strings.isHindi) vehicleEnum.labelHi else vehicleEnum.label

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier.fillMaxWidth().testTag("admin_booking_card_${booking.id}"),
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          PaymentStatusBadge(paymentStatusLabel = booking.paymentStatus, isHindi = strings.isHindi)
          BookingStatusBadge(statusLabel = booking.status, isHindi = strings.isHindi)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            Icons.Filled.Person,
            contentDescription = null,
            tint = NavyDeep,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = booking.customerName,
              style = MaterialTheme.typography.titleMedium,
              color = NavyDeep,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = booking.customerPhone,
              style = MaterialTheme.typography.bodyMedium,
              color = EmeraldPrimary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        Button(
          onClick = onCallCustomer,
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("admin_call_customer_${booking.id}"),
        ) {
          Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.callCustomer, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Spacer(modifier = Modifier.height(10.dp))

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
          fontWeight = FontWeight.Medium,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
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
          fontWeight = FontWeight.Medium,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            Icons.Filled.LocalShipping,
            contentDescription = null,
            tint = NavyDeep,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = vehicleDisplay,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = NavyDeep,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${booking.bookingDateLabel} • ${booking.bookingTimeLabel}",
            style = MaterialTheme.typography.bodySmall,
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
          text = "${booking.goodsDescription} • ${booking.goodsWeight}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (booking.specialInstructions.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${strings.instructionsShort}: ${booking.specialInstructions}",
          style = MaterialTheme.typography.bodySmall,
          color = NavyDeep,
        )
      }

      if (booking.goodsPhotos.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        GoodsPhotoStrip(photosBase64 = booking.goodsPhotos)
      }

      if (booking.estimatedFare.isNotBlank() || booking.paidAmount > 0 || booking.totalFareAmount > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = buildString {
            if (booking.estimatedFare.isNotBlank()) {
              append("${strings.estimatedFareLabel}: ${booking.estimatedFare}")
            } else if (booking.totalFareAmount > 0) {
              append("${strings.estimatedFareLabel}: ₹${booking.totalFareAmount.toLong()}")
            }
            if (booking.paidAmount > 0) {
              if (isNotEmpty()) append(" • ")
              append("${strings.paidAmountLabel}: ₹${booking.paidAmount.toLong()}")
            }
          },
          style = MaterialTheme.typography.labelLarge,
          color = EmeraldPrimary,
          fontWeight = FontWeight.Bold,
        )
      }

      if (booking.assignedVehicleNumber.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          color = NavyContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${strings.vehicleNoLabel}: ${booking.assignedVehicleNumber}",
                style = MaterialTheme.typography.labelLarge,
                color = NavyDeep,
                fontWeight = FontWeight.Bold,
              )
              if (booking.assignedDriverName.isNotBlank()) {
                Text(
                  text = "${booking.assignedDriverName} (${booking.assignedDriverPhone})",
                  style = MaterialTheme.typography.bodySmall,
                  color = NavyOnContainer,
                )
              }
            }
            if (booking.assignedDriverPhone.isNotBlank()) {
              IconButton(onClick = onCallDriver) {
                Icon(Icons.Filled.Call, contentDescription = strings.callDriver, tint = EmeraldPrimary)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (status == BookingStatus.PENDING) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onAccept,
            modifier = Modifier.weight(1f).testTag("admin_confirm_${booking.id}"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.acceptConfirm, fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f).testTag("admin_reject_${booking.id}"),
            border = BorderStroke(1.dp, ErrorRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.rejectCancel)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onAssignVehicleClick,
          modifier = Modifier.weight(1f).testTag("admin_assign_btn_${booking.id}"),
          colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.assignVehicleBtn)
        }
        OutlinedButton(
          onClick = onUpdateStatusClick,
          modifier = Modifier.weight(1f).testTag("admin_status_btn_${booking.id}"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.updateStatusBtn)
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onPaymentClick,
          modifier = Modifier.weight(1f).testTag("admin_payment_btn_${booking.id}"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.recordPaymentBtn)
        }
        OutlinedButton(
          onClick = onNotifyClick,
          modifier = Modifier.weight(1f).testTag("admin_notify_btn_${booking.id}"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.sendNotificationBtn)
        }
      }
    }
  }
}
