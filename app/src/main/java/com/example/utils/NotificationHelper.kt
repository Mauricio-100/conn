package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.mutableStateOf

object NotificationRouter {
    val pendingRoute = mutableStateOf<String?>(null)
}

object NotificationHelper {
    private const val CHANNEL_ID = "cmo_activity_channel"
    private const val CHANNEL_NAME = "Activités CMO"
    private const val CHANNEL_DESC = "Notifications CMO (Likes, Commentaires, Messages, Abonnements)"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    suspend fun showSystemNotification(
        context: Context,
        notificationId: String,
        title: String,
        text: String,
        route: String,
        avatarUrl: String? = null,
        senderName: String? = null
    ) {
        withContext(Dispatchers.IO) {
            initChannels(context)

            // Convert string notificationId to an integer hash for NotificationManager
            val idHash = notificationId.hashCode()

            // Intent to open MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", route)
            }

            // Create PendingIntent
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, idHash, intent, flags)

            // Generate/Load large icon (avatar)
            val largeIcon = if (!avatarUrl.isNullOrEmpty()) {
                downloadAvatarOrPlaceholder(avatarUrl, senderName ?: "?")
            } else {
                generatePlaceholderAvatar(senderName ?: "?")
            }

            // Determine small icon
            val smallIconRes = R.drawable.ic_launcher_foreground

            // Build beautiful custom notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setColor(Color.parseColor("#1877F2")) // Beautiful Facebook Blue branding color
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE) // No standard system default sound
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setSubText("S-3 CMO")

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(idHash, builder.build())

            // Play cute kitten meow sound!
            CatSoundPlayer.playCuteMeow()
        }
    }

    private fun downloadAvatarOrPlaceholder(avatarUrl: String, senderName: String): Bitmap {
        return try {
            val url = URL(avatarUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()
            val input: InputStream = connection.inputStream
            val original = BitmapFactory.decodeStream(input)
            if (original != null) {
                getCircularBitmap(original)
            } else {
                generatePlaceholderAvatar(senderName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            generatePlaceholderAvatar(senderName)
        }
    }

    private fun generatePlaceholderAvatar(name: String): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a soft colored circular background based on sender's name hash
        val paint = Paint().apply {
            isAntiAlias = true
            color = getSoftColorForName(name)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw initials text
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, yPos, textPaint)

        return bitmap
    }

    private fun getCircularBitmap(src: Bitmap): Bitmap {
        val size = Math.min(src.width, src.height)
        val dst = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dst)
        
        val paint = Paint().apply {
            isAntiAlias = true
        }
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val rect = Rect(0, 0, size, size)
        val srcRect = Rect((src.width - size) / 2, (src.height - size) / 2, (src.width + size) / 2, (src.height + size) / 2)
        canvas.drawBitmap(src, srcRect, rect, paint)
        return dst
    }

    private fun getSoftColorForName(name: String): Int {
        val colors = intArrayOf(
            Color.parseColor("#1877F2"), // Facebook Blue
            Color.parseColor("#42B72A"), // Green
            Color.parseColor("#E4405F"), // Pink
            Color.parseColor("#FF9900"), // Yellow-orange
            Color.parseColor("#7F3DFF"), // Purple
            Color.parseColor("#00C6FF")  // Cyan
        )
        val index = Math.abs(name.hashCode()) % colors.size
        return colors[index]
    }
}
