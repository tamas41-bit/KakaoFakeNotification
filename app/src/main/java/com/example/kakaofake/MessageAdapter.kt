package com.example.kakaofake

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val profileProvider: () -> Bitmap?,
    private val onDelete: (ChatMessage) -> Unit,
    private val onSetTime: (ChatMessage) -> Unit
) : RecyclerView.Adapter<MessageAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgProfile: ImageView = v.findViewById(R.id.imgMsgProfile)
        val tvMessage: TextView = v.findViewById(R.id.tvMessageText)
        val tvTime: TextView = v.findViewById(R.id.tvScheduledTime)
        val btnSetTime: ImageButton = v.findViewById(R.id.btnSetTime)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = messages[position]
        holder.tvMessage.text = msg.text

        if (msg.isScheduled) {
            holder.tvTime.text = "⏰ ${msg.timeString()} 예약됨"
            holder.tvTime.setTextColor(0xFF2E7D32.toInt())
        } else if (msg.hour >= 0) {
            holder.tvTime.text = "⏰ ${msg.timeString()}"
            holder.tvTime.setTextColor(0xFF555555.toInt())
        } else {
            holder.tvTime.text = "시간 미설정"
            holder.tvTime.setTextColor(0xFFAAAAAA.toInt())
        }

        val bmp = profileProvider()
        if (bmp != null) holder.imgProfile.setImageBitmap(bmp)
        else holder.imgProfile.setImageResource(R.drawable.ic_default_profile)

        holder.btnSetTime.setOnClickListener { onSetTime(msg) }
        holder.btnDelete.setOnClickListener { onDelete(msg) }
    }

    override fun getItemCount() = messages.size
}
