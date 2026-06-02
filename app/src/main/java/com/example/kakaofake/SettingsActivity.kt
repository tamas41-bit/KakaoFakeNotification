package com.example.kakaofake

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var etRoomName: EditText
    private lateinit var tvRoomLabel: TextView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imgProfile.setImageURI(it)
            try {
                val inputStream = contentResolver.openInputStream(it) ?: return@let
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                val file = File(filesDir, "profile_image.jpg")
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush(); out.close()
                Toast.makeText(this, "프로필 사진이 저장되었습니다", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "사진 저장 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        imgProfile = findViewById(R.id.imgSettingsProfile)
        etName = findViewById(R.id.etSettingsName)
        spinnerType = findViewById(R.id.spinnerSettingsType)
        etRoomName = findViewById(R.id.etSettingsRoomName)
        tvRoomLabel = findViewById(R.id.tvSettingsRoomLabel)

        // Load existing values
        etName.setText(MessageStore.loadSenderName(this))

        val profileFile = File(filesDir, "profile_image.jpg")
        if (profileFile.exists()) {
            imgProfile.setImageBitmap(BitmapFactory.decodeFile(profileFile.absolutePath))
        }

        val types = arrayOf("1:1 채팅", "오픈채팅")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.setSelection(MessageStore.loadChatType(this))
        etRoomName.setText(MessageStore.loadRoomName(this))

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                val isOpen = pos == 1
                tvRoomLabel.visibility = if (isOpen) android.view.View.VISIBLE else android.view.View.GONE
                etRoomName.visibility = if (isOpen) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Trigger initial visibility
        val isOpen = MessageStore.loadChatType(this) == 1
        tvRoomLabel.visibility = if (isOpen) android.view.View.VISIBLE else android.view.View.GONE
        etRoomName.visibility = if (isOpen) android.view.View.VISIBLE else android.view.View.GONE

        findViewById<android.widget.Button>(R.id.btnPickSettingsImage).setOnClickListener {
            pickImage.launch("image/*")
        }

        findViewById<android.widget.Button>(R.id.btnSaveSettings).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(this, "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            MessageStore.saveSenderName(this, name)
            MessageStore.saveChatType(this, spinnerType.selectedItemPosition)
            MessageStore.saveRoomName(this, etRoomName.text.toString().trim())
            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
            finish()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
