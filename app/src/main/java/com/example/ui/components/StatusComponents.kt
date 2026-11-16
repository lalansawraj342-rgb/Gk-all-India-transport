package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BookingStatus
import com.example.ui.theme.AmberAccent
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

data class StatusStyle(
  val containerColor: Color,
  val contentColor: Color,
  val dotColor: Color,
  val icon: ImageVector,
)

fun statusStyleFor(status: BookingStatus): StatusStyle =
  when (status) {
    BookingStatus.NEW_BOOKING ->
      StatusStyle(NavyContainer, NavyOnContainer, NavyDeep, Icons.Filled.FiberNew)
    BookingStatus.PENDING ->
      StatusStyle(AmberContainer, AmberOnContainer, AmberAccent, Icons.Filled.HourglassTop)
    BookingStatus.CONFIRMED ->
      StatusStyle(
        EmeraldLightContainer,
        EmeraldOnContainer,
        EmeraldPrimary,
        Icons.Filled.CheckCircle,
      )
    BookingStatus.VEHICLE_ASSIGNED ->
      StatusStyle(NavyContainer, NavyOnContainer, Color(0xFF1D4ED8), Icons.Filled.DirectionsCar)
    BookingStatus.VEHICLE_DISPATCHED ->
      StatusStyle(
        EmeraldLightContainer,
        EmeraldOnContainer,
        EmeraldPrimary,
        Icons.Filled.LocalShipping,
      )
    BookingStatus.DELIVERED ->
      StatusStyle(Color(0xFFD1FAE5), Color(0xFF065F46), Color(0xFF059669), Icons.Filled.Verified)
    BookingStatus.CANCELLED ->
      StatusStyle(ErrorContainerLight, ErrorOnContainerLight, ErrorRed, Icons.Filled.Cancel)
  }

@Composable
fun BookingStatusBadge(
  statusLabel: String,
  isHindi: Boolean,
  modifier: Modifier = Modifier,
) {
  val status = BookingStatus.fromLabel(statusLabel)
  val style = statusStyleFor(status)
  val displayText = if (isHindi) "${status.labelHi} (${status.label})" else status.label

  Surface(
    color = style.containerColor,
    shape = RoundedCornerShape(20.dp),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(style.dotColor))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = displayText,
        color = style.contentColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

fun dialPhoneNumber(context: Context, phoneNumber: String) {
  val cleaned = phoneNumber.trim()
  if (cleaned.isEmpty()) return
  val intent =
    Intent(Intent.ACTION_DIAL).apply {
      data = Uri.parse("tel:$cleaned")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    // No dialer available on device
  }
}
