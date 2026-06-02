package com.example.kakaofake

data class ChatMessage(
    val id: Int,
    val text: String,
    val hour: Int = -1,
    val minute: Int = -1,
    val second: Int = -1,
    val isScheduled: Boolean = false
) {
    fun timeString(): String = "%02d:%02d:%02d".format(hour, minute, second)

    fun hasTime(): Boolean = hour >= 0

    fun toStorageString(): String = "$id|$text|$hour|$minute|$second|$isScheduled"

    companion object {
        fun fromStorageString(s: String): ChatMessage? {
            return try {
                val p = s.split("|", limit = 6)
                ChatMessage(p[0].toInt(), p[1], p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toBoolean())
            } catch (e: Exception) { null }
        }
    }
}
