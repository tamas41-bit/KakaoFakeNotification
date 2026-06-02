package com.example.kakaofake

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var tvSenderName: TextView
    private lateinit var imgHeaderProfile: ImageView
    private lateinit var etInput: EditText
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()
        NotificationHelper.createChannel(this)

        tvSenderName = findViewById(R.id.tvSenderName)
        imgHeaderProfile = findViewById(R.id.imgHeaderProfile)
        etInput = findViewById(R.id.etInput)

        recyclerView = findViewById(R.id.recyclerView)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager

        adapter = MessageAdapter(
            messages,
            profileProvider = { loadProfileBitmap() },
            onDelete = { deleteMessage(it) },
            onSetTime = { showTimeDialog(it) }
        )
        recyclerView.adapter = adapter

        // 전송 버튼
        findViewById<ImageButton>(R.id.btnSend).setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener
            val now = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }
            val msg = ChatMessage(
                id = MessageStore.nextId(this),
                text = text,
                hour = now.get(Calendar.HOUR_OF_DAY),
                minute = now.get(Calendar.MINUTE),
                second = now.get(Calendar.SECOND)
            )
            messages.add(msg)
            MessageStore.save(this, messages)
            adapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
            etInput.setText("")
            hideKeyboard()
        }

        // 설정 버튼
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 전체 예약
        findViewById<Button>(R.id.btnScheduleAll).setOnClickListener {
            scheduleAll()
        }

        // 전체 취소
        findViewById<Button>(R.id.btnCancelAll).setOnClickListener {
            cancelAll()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHeader()
        messages.clear()
        messages.addAll(MessageStore.load(this))
        adapter.notifyDataSetChanged()
        if (messages.isNotEmpty()) recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun refreshHeader() {
        tvSenderName.text = MessageStore.loadSenderName(this)
        val bmp = loadProfileBitmap()
        if (bmp != null) imgHeaderProfile.setImageBitmap(bmp)
        else imgHeaderProfile.setImageResource(R.drawable.ic_default_profile)
    }

    private fun loadProfileBitmap(): Bitmap? {
        return try {
            val file = File(filesDir, "profile_image.jpg")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (e: Exception) { null }
    }

    // 시간 설정 다이얼로그
    private fun showTimeDialog(msg: ChatMessage) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_time, null)
        val etHour = view.findViewById<EditText>(R.id.etDialogHour)
        val etMin  = view.findViewById<EditText>(R.id.etDialogMin)
        val etSec  = view.findViewById<EditText>(R.id.etDialogSec)

        if (msg.hasTime()) {
            etHour.setText(msg.hour.toString())
            etMin.setText(msg.minute.toString())
            etSec.setText(msg.second.toString())
        }

        AlertDialog.Builder(this)
            .setTitle("알림 시간 설정")
            .setView(view)
            .setPositiveButton("확인") { _, _ ->
                val h = etHour.text.toString().toIntOrNull()
                val m = etMin.text.toString().toIntOrNull()
                val s = etSec.text.toString().toIntOrNull()

                if (h == null || m == null || s == null ||
                    h !in 0..23 || m !in 0..59 || s !in 0..59) {
                    Toast.makeText(this, "올바른 시간을 입력하세요", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val idx = messages.indexOfFirst { it.id == msg.id }
                if (idx >= 0) {
                    messages[idx] = msg.copy(hour = h, minute = m, second = s, isScheduled = false)
                    MessageStore.save(this, messages)
                    adapter.notifyItemChanged(idx)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteMessage(msg: ChatMessage) {
        cancelSingle(msg)
        val idx = messages.indexOfFirst { it.id == msg.id }
        if (idx >= 0) {
            messages.removeAt(idx)
            MessageStore.save(this, messages)
            adapter.notifyItemRemoved(idx)
        }
    }

    private fun scheduleAll() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(this, "정확한 알람 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }

        val senderName = MessageStore.loadSenderName(this)
        val roomName   = MessageStore.loadRoomName(this)
        val isOpen     = MessageStore.loadChatType(this) == 1

        var count = 0
        val noTime = mutableListOf<Int>()

        messages.forEachIndexed { idx, msg ->
            if (msg.hasTime()) {
                scheduleOne(msg, senderName, roomName, isOpen)
                messages[idx] = msg.copy(isScheduled = true)
                count++
            } else {
                noTime.add(idx + 1)
            }
        }

        MessageStore.save(this, messages)
        adapter.notifyDataSetChanged()

        if (noTime.isNotEmpty()) {
            Toast.makeText(this, "${count}개 예약됨. ${noTime.size}개는 시간 미설정", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "${count}개 알림 예약 완료", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAll() {
        messages.forEachIndexed { idx, msg ->
            cancelSingle(msg)
            messages[idx] = msg.copy(isScheduled = false)
        }
        MessageStore.save(this, messages)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "전체 예약 취소됨", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleOne(msg: ChatMessage, senderName: String, roomName: String, isOpen: Boolean) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("senderName", senderName)
            putExtra("roomName", roomName)
            putExtra("message", msg.text)
            putExtra("isOpen", isOpen)
            putExtra("notifId", msg.id)
        }
        val pi = PendingIntent.getBroadcast(
            this, msg.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, msg.hour)
            set(Calendar.MINUTE, msg.minute)
            set(Calendar.SECOND, msg.second)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    private fun cancelSingle(msg: ChatMessage) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this, msg.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etInput.windowToken, 0)
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (perms.isNotEmpty()) ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
    }
}
