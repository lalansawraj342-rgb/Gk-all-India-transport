package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

fun encodePhotoUriToBase64(context: Context, uri: Uri, maxDimPx: Int = 360): String? {
  return try {
    val input = context.contentResolver.openInputStream(uri) ?: return null
    val original = input.use { BitmapFactory.decodeStream(it) } ?: return null

    val width = original.width
    val height = original.height
    val scale =
      if (width > maxDimPx || height > maxDimPx) {
        maxDimPx.toFloat() / maxOf(width, height).toFloat()
      } else {
        1f
      }
    val targetW = (width * scale).toInt().coerceAtLeast(1)
    val targetH = (height * scale).toInt().coerceAtLeast(1)
    val resized =
      if (scale < 1f) Bitmap.createScaledBitmap(original, targetW, targetH, true) else original

    val out = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 60, out)
    if (resized !== original) resized.recycle()
    original.recycle()

    val bytes = out.toByteArray()
    Base64.encodeToString(bytes, Base64.NO_WRAP)
  } catch (_: Exception) {
    null
  }
}

fun decodeBase64ToImageBitmap(encoded: String): ImageBitmap? {
  return try {
    val clean = encoded.substringAfter("base64,", encoded)
    val bytes = Base64.decode(clean, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
  } catch (_: Exception) {
    null
  }
}
