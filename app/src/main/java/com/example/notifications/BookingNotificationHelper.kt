package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.Booking

object BookingNotificationHelper {
  private const val CHANNEL_ID = "gk_transport_alerts"
  private const val CHANNEL_NAME = "G.K. All Transport Alerts"
  private const val CHANNEL_DESC = "Booking requests, approvals, vehicle assignment, and delivery alerts"

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
          description = CHANNEL_DESC
          enableVibration(true)
        }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun notifyNewBooking(context: Context, booking: Booking, isHindi: Boolean) {
    val title =
      if (isHindi) {
        "नई बुकिंग: ${booking.vehicleType} (${booking.bookingCode})"
      } else {
        "New Booking: ${booking.vehicleType} (${booking.bookingCode})"
      }
    val body =
      "${booking.customerName} (${booking.customerPhone}) • ${booking.pickupLocation} → ${booking.deliveryLocation}"
    showNotification(context, booking.id.hashCode(), title, body)
  }

  fun notifyAppNotification(context: Context, notification: AppNotification) {
    showNotification(
      context = context,
      notificationId = notification.id.hashCode(),
      title = notification.title,
      body = notification.message,
    )
  }

  fun showNotification(context: Context, notificationId: Int, title: String, body: String) {
    ensureChannel(context)
    if (!hasNotificationPermission(context)) return

    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Permission revoked concurrently
    }
  }
}
