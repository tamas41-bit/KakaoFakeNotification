package com.example.kakaofake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val senderName = intent.getStringExtra("senderName") ?: "카카오"
        val roomName   = intent.getStringExtra("roomName")   ?: ""
        val message    = intent.getStringExtra("message")    ?: ""
        val isOpen     = intent.getBooleanExtra("isOpen", false)
        val notifId    = intent.getIntExtra("notifId", 1001)

        val profileBitmap = loadCircularProfile(context)

        val displayName = if (isOpen && roomName.isNotBlank()) roomName else senderName
        val timeStr = SimpleDateFormat("a h:mm", Locale.KOREA).format(Date())

        // 커스텀 알림 레이아웃 (잠금화면 포함 모든 곳에 프로필 사진 왼쪽 표시)
        val customView = RemoteViews(context.packageName, R.layout.notification_custom)
        customView.setTextViewText(R.id.notif_sender, displayName)
        customView.setTextViewText(R.id.notif_message, message)
        customView.setTextViewText(R.id.notif_time, timeStr)
        if (profileBitmap != null) {
            customView.setImageViewBitmap(R.id.notif_profile, profileBitmap)
        } else {
            customView.setImageViewResource(R.id.notif_profile, R.drawable.ic_default_profile)
        }

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .apply {
                if (profileBitmap != null) {
                    setSmallIcon(IconCompat.createWithBitmap(profileBitmap))
                } else {
                    setSmallIcon(R.drawable.ic_kakao_notification)
                }
            }
            .setCustomContentView(customView)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    private fun loadCircularProfile(context: Context): Bitmap? {
        return try {
            val file = File(context.filesDir, "profile_image.jpg")
            if (!file.exists()) return null
            val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            makeCircular(raw)
        } catch (e: Exception) { null }
    }

    private fun makeCircular(src: Bitmap): Bitmap {
        // 중앙 기준으로 정사각형 크롭
        val size = minOf(src.width, src.height)
        val xOffset = (src.width - size) / 2
        val yOffset = (src.height - size) / 2
        val cropped = Bitmap.createBitmap(src, xOffset, yOffset, size, size)

        // 256x256으로 리사이즈
        val scaled = Bitmap.createScaledBitmap(cropped, 256, 256, true)

        // 원형으로 마스킹
        val output = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(128f, 128f, 128f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return output
    }
}
