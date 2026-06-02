package com.example.kakaofake

import android.content.Context

object MessageStore {

    private const val PREF = "messages"
    private const val KEY = "list"
    private const val KEY_COUNTER = "counter"

    fun load(context: Context): MutableList<ChatMessage> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.split("\n").mapNotNull { ChatMessage.fromStorageString(it) }.toMutableList()
    }

    fun save(context: Context, messages: List<ChatMessage>) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, messages.joinToString("\n") { it.toStorageString() }).apply()
    }

    fun nextId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_COUNTER, 1)
        prefs.edit().putInt(KEY_COUNTER, next + 1).apply()
        return next
    }

    fun loadSenderName(context: Context): String {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("senderName", "카카오친구") ?: "카카오친구"
    }

    fun saveSenderName(context: Context, name: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("senderName", name).apply()
    }

    fun loadChatType(context: Context): Int {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt("chatType", 0)
    }

    fun saveChatType(context: Context, type: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putInt("chatType", type).apply()
    }

    fun loadRoomName(context: Context): String {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("roomName", "") ?: ""
    }

    fun saveRoomName(context: Context, name: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("roomName", name).apply()
    }
}
