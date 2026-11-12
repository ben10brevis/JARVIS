package com.jarvis.assistant.skills

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.app.NotificationManager
import android.provider.AlarmClock
import android.provider.Settings
import com.jarvis.assistant.core.AssistantResponse
import com.jarvis.assistant.core.ResponseStatus
import com.jarvis.assistant.notifications.NotificationStore
import java.util.Locale

/** Device skills use public Android APIs and explicit user-granted special access. */
class LocalCommandRouter(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun execute(raw: String): AssistantResponse {
        val text = raw.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return response("I didn't catch that. Please try again.", ResponseStatus.ERROR)

        return when {
            Regex("""^(?:call|dial)\\s+\\+?[0-9][0-9 -]{4,18}$""").matches(text) -> {
                val number = raw.trim().substringAfter(' ').trim()
                openScreen(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))), "Opening the dialer. Review the number and tap Call to place the call.")
            }
            Regex("""^message\\s+\\+?[0-9][0-9 -]{4,18}\\s+.+$""").matches(text) -> {
                val parts = raw.trim().split(Regex("\\s+"), limit = 3)
                val number = parts.getOrNull(1).orEmpty()
                val body = parts.getOrNull(2).orEmpty()
                openScreen(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))).putExtra("sms_body", body), "Opening your messaging app. Review and send the message yourself.")
            }
            containsAny(text, "open camera", "camera open", "కెమెరా ఓపెన్") ->
                openScreen(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "Opening the camera.")

            containsAny(text, "flashlight on", "turn on flashlight", "torch on", "టార్చ్ ఆన్", "లైట్ ఆన్") -> setTorch(true)
            containsAny(text, "flashlight off", "turn off flashlight", "torch off", "టార్చ్ ఆఫ్", "లైట్ ఆఫ్") -> setTorch(false)

            text.contains("brightness") || text.contains("బ్రైట్‌నెస్") -> setBrightness(text)
            containsAny(text, "allow brightness", "brightness permission") -> openScreen(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(android.net.Uri.parse("package:" + context.packageName)),
                "Allow JARVIS to modify system settings on the next screen."
            )

            containsAny(text, "enable notification access", "notification permission", "నోటిఫికేషన్ యాక్సెస్") ->
                openScreen(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), "Enable JARVIS notification access in Android settings if you want me to read recent notifications.")

            containsAny(text, "read notifications", "read my notifications", "నా నోటిఫికేషన్స్ చదువు", "నోటిఫికేషన్స్ చదువు") -> readNotifications()
            containsAny(text, "clear jarvis notification memory", "forget recent notifications") -> {
                NotificationStore.clear()
                response("JARVIS cleared its temporary notification list.", ResponseStatus.SUCCESS)
            }

            containsAny(text, "allow do not disturb", "dnd permission", "dnd access") ->
                openScreen(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), "Grant Do Not Disturb access to JARVIS if you want voice control of this setting.")
            containsAny(text, "turn on do not disturb", "enable do not disturb", "dnd on") -> setDnd(true)
            containsAny(text, "turn off do not disturb", "disable do not disturb", "dnd off") -> setDnd(false)

            containsAny(text, "volume up", "increase volume", "వాల్యూమ్ పెంచు", "సౌండ్ పెంచు") -> adjustVolume(AudioManager.ADJUST_RAISE, "Volume increased.")
            containsAny(text, "volume down", "decrease volume", "వాల్యూమ్ తగ్గించు", "సౌండ్ తగ్గించు") -> adjustVolume(AudioManager.ADJUST_LOWER, "Volume decreased.")
            containsAny(text, "mute volume", "mute sound", "మ్యూట్") -> adjustVolume(AudioManager.ADJUST_MUTE, "Media volume muted.")

            containsAny(text, "set alarm", "create alarm", "అలారం పెట్టు", "అలారం సెట్") -> setAlarm(text)
            containsAny(text, "set timer", "start timer", "టైమర్ పెట్టు", "టైమర్ సెట్") -> setTimer(text)

            containsAny(text, "open youtube", "youtube open", "యూట్యూబ్ ఓపెన్", "youtube") -> launchApp("com.google.android.youtube", "YouTube")

            containsAny(text, "wifi", "wi-fi", "వైఫై", "వై ఫై") ->
                openScreen(Intent(Settings.Panel.ACTION_WIFI), "Opening Wi-Fi controls. Confirm changes on the system panel.")
            containsAny(text, "internet settings", "mobile data", "airplane mode", "hotspot") ->
                openScreen(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY), "Opening Internet connectivity controls. Android requires you to confirm changes there.")
            containsAny(text, "bluetooth", "బ్లూటూత్") ->
                openScreen(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Opening Bluetooth settings.")
            containsAny(text, "brightness settings", "display settings") ->
                openScreen(Intent(Settings.ACTION_DISPLAY_SETTINGS), "Opening display settings.")
            containsAny(text, "sound settings", "audio settings", "సౌండ్ సెట్టింగ్స్") ->
                openScreen(Intent(Settings.ACTION_SOUND_SETTINGS), "Opening sound settings.")
            containsAny(text, "notification settings", "నోటిఫికేషన్ సెట్టింగ్స్") ->
                openScreen(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName), "Opening JARVIS notification settings.")
            containsAny(text, "open settings", "settings open", "సెట్టింగ్స్ ఓపెన్") ->
                openScreen(Intent(Settings.ACTION_SETTINGS), "Opening Settings.")

            text.startsWith("search ") || text.startsWith("google ") -> {
                val query = raw.trim().substringAfter(' ').trim()
                if (query.isBlank()) response("Tell me what you want to search for.", ResponseStatus.ERROR)
                else openScreen(Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query), "Searching the web for $query.")
            }
            text.startsWith("open ") -> launchAppByLabel(raw.trim().substringAfter(' ').trim())
            else -> response("Try: flashlight on, brightness 50 percent, read notifications, volume up, Wi-Fi, Bluetooth, set alarm, or open an app.", ResponseStatus.INFO)
        }
    }

    private fun setTorch(enabled: Boolean): AssistantResponse = try {
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return response("This device does not expose a flashlight to Android.", ResponseStatus.ERROR)
        cameraManager.setTorchMode(cameraId, enabled)
        response(if (enabled) "Flashlight turned on." else "Flashlight turned off.", ResponseStatus.SUCCESS)
    } catch (_: Exception) {
        response("Android could not change the flashlight. Check whether another app is using the camera.", ResponseStatus.ERROR)
    }

    private fun setBrightness(text: String): AssistantResponse {
        val percent = Regex("""(?:brightness|బ్రైట్‌నెస్)(?:\s+(?:to|at))?\s+(\d{1,3})""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        if (percent == null) return openScreen(Intent(Settings.ACTION_DISPLAY_SETTINGS), "Say a percentage, for example: brightness 50 percent. Opening display settings.")
        if (percent !in 1..100) return response("Brightness must be between 1 and 100 percent.", ResponseStatus.ERROR)
        if (!Settings.System.canWrite(context)) {
            return openScreen(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(android.net.Uri.parse("package:" + context.packageName)), "Allow JARVIS to modify system settings, then repeat the brightness command.")
        }
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (percent * 255 / 100).coerceIn(1, 255))
            response("Screen brightness set to $percent percent.", ResponseStatus.SUCCESS)
        } catch (_: Exception) {
            response("Android or the device manufacturer blocked direct brightness control. Use Display settings.", ResponseStatus.ERROR)
        }
    }

    private fun readNotifications(): AssistantResponse {
        val items = NotificationStore.latest(5)
        if (items.isEmpty()) return response(
            "No recent notifications are available. Enable JARVIS Notification Access in Android settings, then try again. Notification content is kept temporarily in memory on this device.",
            ResponseStatus.INFO
        )
        return response("Your latest notifications: " + items.joinToString(". "), ResponseStatus.SUCCESS)
    }

    private fun setDnd(enabled: Boolean): AssistantResponse {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return openScreen(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), "Grant Do Not Disturb access to JARVIS, then repeat the command.")
        }
        return try {
            notificationManager.setInterruptionFilter(
                if (enabled) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            response(if (enabled) "Do Not Disturb enabled." else "Do Not Disturb disabled.", ResponseStatus.SUCCESS)
        } catch (_: Exception) {
            response("Android did not allow changing Do Not Disturb on this device.", ResponseStatus.ERROR)
        }
    }

    private fun adjustVolume(direction: Int, message: String): AssistantResponse = try {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        response(message, ResponseStatus.SUCCESS)
    } catch (_: Exception) {
        response("Android blocked that volume change. Use the phone's volume controls.", ResponseStatus.ERROR)
    }

    private fun setAlarm(text: String): AssistantResponse {
        val match = Regex("""(?:set|create)?\s*alarm(?:\s+at|\s+for)?\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""").find(text)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: return response("Please say a valid alarm time, such as 6:30 AM.", ResponseStatus.ERROR)
            val minute = match.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: 0
            if (hour !in 1..12 || minute !in 0..59) return response("That alarm time doesn't look valid.", ResponseStatus.ERROR)
            when (match.groupValues[3]) {
                "pm" -> if (hour != 12) hour += 12
                "am" -> if (hour == 12) hour = 0
            }
            intent.putExtra(AlarmClock.EXTRA_HOUR, hour)
            intent.putExtra(AlarmClock.EXTRA_MINUTES, minute)
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS alarm")
            return openScreen(intent, "Opening the alarm confirmation screen.")
        }
        return openScreen(Intent(AlarmClock.ACTION_SHOW_ALARMS), "Opening your alarms.")
    }

    private fun setTimer(text: String): AssistantResponse {
        val match = Regex("""(?:set|start)?\s*timer(?:\s+for)?\s+(\d+)\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)""").find(text)
            ?: return openScreen(Intent(AlarmClock.ACTION_SET_TIMER), "Opening the timer screen.")
        val amount = match.groupValues[1].toLongOrNull() ?: return response("Please give me a valid timer duration.", ResponseStatus.ERROR)
        val unit = match.groupValues[2]
        val multiplier = when {
            unit.startsWith("hour") || unit.startsWith("hr") -> 3600L
            unit.startsWith("min") -> 60L
            else -> 1L
        }
        val seconds = amount * multiplier
        if (seconds !in 1..86400) return response("Please choose a timer between 1 second and 24 hours.", ResponseStatus.ERROR)
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, seconds.toInt())
            .putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS timer")
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        return openScreen(intent, "Opening the timer confirmation screen.")
    }

    private fun launchApp(packageName: String, label: String): AssistantResponse {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return response("$label is not installed or its launch activity is unavailable.", ResponseStatus.ERROR)
        return openScreen(launch, "Opening $label.")
    }

    private fun launchAppByLabel(label: String): AssistantResponse {
        if (label.isBlank()) return response("Tell me which app to open.", ResponseStatus.ERROR)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val candidates = try { context.packageManager.queryIntentActivities(launcherIntent, 0) } catch (_: Exception) { emptyList() }
        val target = candidates.firstOrNull {
            it.loadLabel(context.packageManager).toString().equals(label, ignoreCase = true) ||
                it.activityInfo.packageName.substringAfterLast('.').equals(label, ignoreCase = true)
        } ?: return response("I couldn't find an app named $label. Try its exact app name.", ResponseStatus.ERROR)
        val launch = context.packageManager.getLaunchIntentForPackage(target.activityInfo.packageName)
            ?: return response("That app doesn't expose a launch screen.", ResponseStatus.ERROR)
        return openScreen(launch, "Opening ${target.loadLabel(context.packageManager)}.")
    }

    private fun openScreen(intent: Intent, message: String): AssistantResponse = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        response(message, ResponseStatus.SUCCESS)
    } catch (_: Exception) {
        response("Android couldn't open that screen or app on this device.", ResponseStatus.ERROR)
    }

    private fun response(message: String, status: ResponseStatus) = AssistantResponse(message, status)
    private fun containsAny(text: String, vararg terms: String) = terms.any(text::contains)
}
