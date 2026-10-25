package com.example.notification

import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object AppNotificationRepository {
    private const val PREFS_NAME = "apex_app_notifications"
    private const val KEY_ITEMS = "notification_items_json"
    private const val MAX_ITEMS = 40

    private val _notifications = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val notifications: StateFlow<List<AppNotificationItem>> = _notifications.asStateFlow()

    private var isInitialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (!isInitialized) {
            _notifications.value = loadFromPrefs(context)
            isInitialized = true
        }
    }

    @Synchronized
    fun addNotification(context: Context, item: AppNotificationItem) {
        initialize(context)
        val current = _notifications.value.toMutableList()
        // Avoid duplicate by id or same notificationId within last 5 seconds
        current.removeAll { it.id == item.id || (it.notificationId == item.notificationId && it.title == item.title) }
        current.add(0, item)
        if (current.size > MAX_ITEMS) {
            val trimmed = current.take(MAX_ITEMS)
            _notifications.value = trimmed
            saveToPrefs(context, trimmed)
        } else {
            _notifications.value = current
            saveToPrefs(context, current)
        }
    }

    @Synchronized
    fun markAsRead(context: Context, notificationId: Int) {
        initialize(context)
        val updated = _notifications.value.map { item ->
            if (item.notificationId == notificationId) item.copy(isRead = true) else item
        }
        _notifications.value = updated
        saveToPrefs(context, updated)

        // Dismiss the notification from Android status bar if active
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)
    }

    @Synchronized
    fun markAsReadById(context: Context, id: String) {
        initialize(context)
        var targetNotifId: Int? = null
        val updated = _notifications.value.map { item ->
            if (item.id == id) {
                targetNotifId = item.notificationId
                item.copy(isRead = true)
            } else item
        }
        _notifications.value = updated
        saveToPrefs(context, updated)

        targetNotifId?.let { notifId ->
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(notifId)
        }
    }

    @Synchronized
    fun markAllAsRead(context: Context) {
        initialize(context)
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        saveToPrefs(context, updated)

        // Dismiss all notifications in Android notification shade
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancelAll()
    }

    @Synchronized
    fun clearAll(context: Context) {
        initialize(context)
        _notifications.value = emptyList()
        saveToPrefs(context, emptyList())

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancelAll()
    }

    private fun loadFromPrefs(context: Context): List<AppNotificationItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<AppNotificationItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AppNotificationItem(
                        id = obj.getString("id"),
                        notificationId = obj.getInt("notificationId"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        muscleGroup = if (obj.has("muscleGroup") && !obj.isNull("muscleGroup")) obj.getString("muscleGroup") else null,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToPrefs(context: Context, list: List<AppNotificationItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("notificationId", item.notificationId)
                    put("title", item.title)
                    put("message", item.message)
                    put("muscleGroup", item.muscleGroup)
                    put("timestamp", item.timestamp)
                    put("isRead", item.isRead)
                }
                jsonArray.put(obj)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ITEMS, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }
}
