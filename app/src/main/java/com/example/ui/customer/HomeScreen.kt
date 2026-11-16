package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.SUPPORT_PHONE_NUMBERS
import com.example.data.model.UserProfile
import com.example.data.model.VehicleType
import com.example.ui.components.BookingStatusBadge
import com.example.ui.components.dialPhoneNumber
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyOnContainer
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  strings: AppStrings,
  userProfile: UserProfile?,
  bookingsState: UiState<List<Booking>>,
  onBookVehicleClick: (VehicleType?) -> Unit,
  onMyBookingsClick: () -> Unit,
  onBookingHistoryClick: () -> Unit,
  onTrackVehicleClick: (Booking?) -> Unit,
  onPaymentsClick: () -> Unit,
  onSupportClick: () -> Unit,
) {
  val context = LocalContext.current
  val bookings = (bookingsState as? UiState.Success)?.data ?: emptyList()
  val activeBooking = bookings.firstOrNull { BookingStatus.fromLabel(it.status).isActiveTrip() }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 760.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Banner with Book a Vehicle CTA
      Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
          Image(
            painter = painterResource(id = R.drawable.img_transport_hero),
            contentDescription = strings.brandTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(NavyDeep.copy(alpha = 0.60f), NavyDeep.copy(alpha = 0.92f))
                  )
                )
          )
          Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(color = EmeraldPrimary, shape = RoundedCornerShape(8.dp)) {
                Text(
                  text = strings.allIndiaServiceBadge,
                  style = MaterialTheme.typography.labelSmall,
                  color = PureWhite,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
              if (userProfile != null && userProfile.name.isNotBlank()) {
                Text(
                  text = userProfile.name,
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.9f),
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = strings.homeHeroHeadline,
                style = MaterialTheme.typography.headlineSmall,
                color = PureWhite,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = strings.homeHeroSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
              )
              Spacer(modifier = Modifier.height(4.dp))
              Button(
                onClick = { onBookVehicleClick(null) },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("home_book_vehicle_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                contentPadding = PaddingValues(horizontal = 18.dp),
              ) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = strings.homeBookVehicleCta,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                )
              }
            }
          }
        }
      }

      // Live Active Shipment Quick Tracker
      if (activeBooking != null) {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = NavyDeep),
          modifier = Modifier.fillMaxWidth().testTag("home_active_tracker"),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${strings.homeActiveShipmentTitle} • ${activeBooking.bookingCode}",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldLightContainer,
                fontWeight = FontWeight.Bold,
              )
              BookingStatusBadge(statusLabel = activeBooking.status, isHindi = strings.isHindi)
            }
            Text(
              text = "${activeBooking.pickupLocation} → ${activeBooking.deliveryLocation}",
              style = MaterialTheme.typography.titleSmall,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (activeBooking.assignedVehicleNumber.isNotBlank()) {
                    "${activeBooking.vehicleType} • ${activeBooking.assignedVehicleNumber}"
                  } else {
                    activeBooking.vehicleType
                  },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
              )
              Button(
                onClick = { onTrackVehicleClick(activeBooking) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Filled.LocationSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.homeTrackNowBtn, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Quick Services Grid
      Text(
        text = strings.homeQuickActions,
        style = MaterialTheme.typography.titleMedium,
        color = NavyDeep,
        fontWeight = FontWeight.Bold,
      )

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
      ) {
        HomeQuickTile(
          icon = Icons.Filled.ListAlt,
          title = strings.homeMyBookingsTile,
          tint = EmeraldPrimary,
          containerColor = EmeraldLightContainer,
          onClick = onMyBookingsClick,
          modifier = Modifier.weight(1f).testTag("home_my_bookings_tile"),
        )
        HomeQuickTile(
          icon = Icons.Filled.History,
          title = strings.homeBookingHistoryTile,
          tint = NavyDeep,
          containerColor = NavyContainer,
          onClick = onBookingHistoryClick,
          modifier = Modifier.weight(1f).testTag("home_history_tile"),
        )
        HomeQuickTile(
          icon = Icons.Filled.LocationSearching,
          title = strings.homeTrackVehicleTile,
          tint = NavyDeep,
          containerColor = NavyContainer,
          onClick = { onTrackVehicleClick(activeBooking) },
          modifier = Modifier.weight(1f).testTag("home_track_tile"),
        )
        HomeQuickTile(
          icon = Icons.Filled.Payments,
          title = strings.homePaymentsTile,
          tint = EmeraldPrimary,
          containerColor = EmeraldLightContainer,
          onClick = onPaymentsClick,
          modifier = Modifier.weight(1f).testTag("home_payments_tile"),
        )
        HomeQuickTile(
          icon = Icons.Filled.SupportAgent,
          title = strings.homeSupportTile,
          tint = PureWhite,
          containerColor = NavyDeep,
          textColor = PureWhite,
          onClick = onSupportClick,
          modifier = Modifier.fillMaxWidth().testTag("home_support_tile"),
        )
      }

      // Available Trucks & Vehicle Categories
      Text(
        text = strings.homeAvailableTrucksTitle,
        style = MaterialTheme.typography.titleMedium,
        color = NavyDeep,
        fontWeight = FontWeight.Bold,
      )

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
      ) {
        VehicleType.entries.forEach { vehicle ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onBookVehicleClick(vehicle) }
                .testTag("home_vehicle_${vehicle.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                color = EmeraldLightContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (strings.isHindi) vehicle.labelHi else vehicle.label,
                  style = MaterialTheme.typography.labelLarge,
                  color = NavyDeep,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = if (strings.isHindi) vehicle.capacityHintHi else vehicle.capacityHint,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      // 24x7 Customer Support Helpline Strip
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = EmeraldPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.supportPhoneLabel,
              style = MaterialTheme.typography.titleMedium,
              color = NavyDeep,
              fontWeight = FontWeight.Bold,
            )
          }
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SUPPORT_PHONE_NUMBERS.forEach { num ->
              Button(
                onClick = { dialPhoneNumber(context, num) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = EmeraldLightContainer,
                    contentColor = EmeraldOnContainer,
                  ),
                shape = RoundedCornerShape(12.dp),
              ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(num, fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun HomeQuickTile(
  icon: ImageVector,
  title: String,
  tint: Color,
  containerColor: Color,
  textColor: Color = NavyOnContainer,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = textColor,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
