package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BookingStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.SUPPORT_PHONE_NUMBERS
import com.example.ui.i18n.AppStrings
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
import com.example.ui.theme.PureWhite

data class StatusStyle(
  val containerColor: Color,
  val contentColor: Color,
  val dotColor: Color,
  val icon: ImageVector,
)

fun statusStyleFor(status: BookingStatus): StatusStyle =
  when (status) {
    BookingStatus.PENDING ->
      StatusStyle(AmberContainer, AmberOnContainer, AmberAccent, Icons.Filled.HourglassTop)
    BookingStatus.ACCEPTED ->
      StatusStyle(
        EmeraldLightContainer,
        EmeraldOnContainer,
        EmeraldPrimary,
        Icons.Filled.CheckCircle,
      )
    BookingStatus.VEHICLE_ASSIGNED ->
      StatusStyle(NavyContainer, NavyOnContainer, Color(0xFF1D4ED8), Icons.Filled.DirectionsCar)
    BookingStatus.DRIVER_ASSIGNED ->
      StatusStyle(NavyContainer, NavyOnContainer, NavyDeep, Icons.Filled.PersonPin)
    BookingStatus.ON_THE_WAY ->
      StatusStyle(NavyContainer, NavyOnContainer, Color(0xFF0284C7), Icons.Filled.LocalShipping)
    BookingStatus.GOODS_PICKED_UP ->
      StatusStyle(AmberContainer, AmberOnContainer, AmberAccent, Icons.Filled.Inventory2)
    BookingStatus.IN_TRANSIT ->
      StatusStyle(
        EmeraldLightContainer,
        EmeraldOnContainer,
        EmeraldPrimary,
        Icons.Filled.Route,
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
  val displayText = if (isHindi) status.labelHi else status.label

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

@Composable
fun PaymentStatusBadge(
  paymentStatusLabel: String,
  isHindi: Boolean,
  modifier: Modifier = Modifier,
) {
  val pStatus = PaymentStatus.fromLabel(paymentStatusLabel)
  val (bg, fg) =
    when (pStatus) {
      PaymentStatus.UNPAID -> ErrorContainerLight to ErrorOnContainerLight
      PaymentStatus.ADVANCE_PAID -> AmberContainer to AmberOnContainer
      PaymentStatus.PAID -> EmeraldLightContainer to EmeraldOnContainer
    }
  val text = if (isHindi) pStatus.labelHi else pStatus.label
  Surface(color = bg, shape = RoundedCornerShape(14.dp), modifier = modifier) {
    Text(
      text = text,
      color = fg,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun BookingStatusTimeline(
  currentStatusLabel: String,
  isHindi: Boolean,
  modifier: Modifier = Modifier,
) {
  val current = BookingStatus.fromLabel(currentStatusLabel)
  if (current == BookingStatus.CANCELLED) {
    Surface(
      color = ErrorContainerLight,
      shape = RoundedCornerShape(12.dp),
      modifier = modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Cancel, contentDescription = null, tint = ErrorRed)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isHindi) current.labelHi else current.label,
          color = ErrorOnContainerLight,
          fontWeight = FontWeight.Bold,
        )
      }
    }
    return
  }

  val steps = BookingStatus.trackableSteps
  val currentIndex = steps.indexOf(current).coerceAtLeast(0)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    steps.forEachIndexed { idx, step ->
      val completed = idx <= currentIndex
      val isCurrent = idx == currentIndex
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(22.dp)
              .clip(CircleShape)
              .background(
                when {
                  isCurrent -> EmeraldPrimary
                  completed -> NavyDeep
                  else -> MaterialTheme.colorScheme.surfaceVariant
                }
              ),
          contentAlignment = Alignment.Center,
        ) {
          if (completed) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(14.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = if (isHindi) step.labelHi else step.label,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = if (isCurrent) FontWeight.ExtraBold else if (completed) FontWeight.SemiBold else FontWeight.Normal,
          color =
            when {
              isCurrent -> EmeraldPrimary
              completed -> NavyDeep
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
      }
    }
  }
}

@Composable
fun GoodsPhotoStrip(
  photosBase64: List<String>,
  modifier: Modifier = Modifier,
) {
  if (photosBase64.isEmpty()) return
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    photosBase64.forEach { encoded ->
      val bmp = remember(encoded) { decodeBase64ToImageBitmap(encoded) }
      if (bmp != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier.size(72.dp),
        ) {
          Image(
            bitmap = bmp,
            contentDescription = "Goods photo",
            contentScale = ContentScale.Crop,
          )
        }
      }
    }
  }
}

@Composable
fun SupportHelplineDialog(
  strings: AppStrings,
  context: Context,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.SupportAgent,
        contentDescription = null,
        tint = EmeraldPrimary,
        modifier = Modifier.size(40.dp),
      )
    },
    title = {
      Text(
        text = strings.customerSupportTitle,
        fontWeight = FontWeight.Bold,
        color = NavyDeep,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = strings.customerSupportSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SUPPORT_PHONE_NUMBERS.forEach { number ->
          Surface(
            color = EmeraldLightContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "+91 $number",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDeep,
              )
              Button(
                onClick = { dialPhoneNumber(context, number) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.callCustomer, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(strings.closeBtn) }
    },
  )
}

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Notifications
import com.example.data.model.AppNotification

@Composable
fun AppNotificationsDialog(
  strings: AppStrings,
  notifications: List<AppNotification>,
  onMarkRead: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.Notifications,
        contentDescription = null,
        tint = EmeraldPrimary,
        modifier = Modifier.size(34.dp),
      )
    },
    title = {
      Text(
        text = strings.notificationsTitle,
        fontWeight = FontWeight.Bold,
        color = NavyDeep,
      )
    },
    text = {
      if (notifications.isEmpty()) {
        Text(
          text = strings.noNotificationsYet,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          notifications.forEach { notif ->
            Surface(
              color =
                if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                else EmeraldLightContainer.copy(alpha = 0.75f),
              shape = RoundedCornerShape(12.dp),
              border =
                if (!notif.isRead) BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)) else null,
              modifier =
                Modifier.fillMaxWidth().clickable {
                  if (!notif.isRead && notif.id.isNotBlank()) {
                    onMarkRead(notif.id)
                  }
                },
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = notif.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = NavyDeep,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                  )
                  if (notif.bookingCode.isNotBlank()) {
                    Text(
                      text = notif.bookingCode,
                      style = MaterialTheme.typography.labelSmall,
                      color = EmeraldPrimary,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
                Text(
                  text = notif.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(strings.closeBtn) }
    },
  )
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
