package com.jarvis.assistant.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Opt-in notification listener. Notification text stays in memory on this device only. */
object NotificationStore {
    private val items = ArrayDeque<String>()

    @Synchronized
    fun add(packageName: String, title: String?, text: String?) {
        val cleanTitle = title?.trim().orEmpty()
        val cleanText = text?.trim().orEmpty()
        if (cleanTitle.isBlank() && cleanText.isBlank()) return
        val label = packageName.substringAfterLast('.').replace('_', ' ')
        val entry = buildString {
            append(label)
            if (cleanTitle.isNotBlank()) append(": ").append(cleanTitle)
            if (cleanText.isNotBlank() && cleanText != cleanTitle) append(" — ").append(cleanText)
        }.take(500)
        items.addFirst(entry)
        while (items.size > 15) items.removeLast()
    }

    @Synchronized
    fun latest(limit: Int = 5): List<String> = items.take(limit)

    @Synchronized
    fun clear() = items.clear()
}

class JarvisNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        NotificationStore.add(sbn.packageName, title, text)
    }
}
