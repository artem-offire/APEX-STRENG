package com.example.notification

data class AppNotificationItem(
    val id: String,
    val notificationId: Int,
    val title: String,
    val message: String,
    val muscleGroup: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
