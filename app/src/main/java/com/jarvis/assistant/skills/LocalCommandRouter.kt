package com.jarvis.assistant.skills

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.AlarmClock
import android.provider.Settings
import com.jarvis.assistant.core.AssistantResponse
import com.jarvis.assistant.core.ResponseStatus
import java.util.Locale

/** Local, on-device skills using public Android APIs and system confirmation screens. */
class LocalCommandRouter(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun execute(raw: String): AssistantResponse {
        val text = raw.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return response("I didn't catch that. Please try again.", ResponseStatus.ERROR)

        return when {
            containsAny(text, "volume up", "increase volume", "వాల్యూమ్ పెంచు", "సౌండ్ పెంచు") -> adjustVolume(AudioManager.ADJUST_RAISE, "Volume increased.")
            containsAny(text, "volume down", "decrease volume", "వాల్యూమ్ తగ్గించు", "సౌండ్ తగ్గించు") -> adjustVolume(AudioManager.ADJUST_LOWER, "Volume decreased.")
            containsAny(text, "mute volume", "mute sound", "మ్యూట్") -> adjustVolume(AudioManager.ADJUST_MUTE, "Media volume muted.")

            containsAny(text, "set alarm", "create alarm", "అలారం పెట్టు", "అలారం సెట్") -> setAlarm(text)
            containsAny(text, "set timer", "start timer", "టైమర్ పెట్టు", "టైమర్ సెట్") -> setTimer(text)

            containsAny(text, "open youtube", "youtube open", "యూట్యూబ్ ఓపెన్", "youtube") ->
                launchApp("com.google.android.youtube", "YouTube")

            containsAny(text, "wifi", "wi-fi", "వైఫై", "వై ఫై") ->
                openScreen(Intent(Settings.Panel.ACTION_WIFI), "Opening Wi-Fi controls. Confirm changes on the system panel.")

            containsAny(text, "bluetooth", "బ్లూటూత్") ->
                openScreen(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Opening Bluetooth settings.")

            containsAny(text, "brightness", "display settings", "బ్రైట్‌నెస్") ->
                openScreen(Intent(Settings.ACTION_DISPLAY_SETTINGS), "Opening display settings.")

            containsAny(text, "sound settings", "audio settings", "సౌండ్ సెట్టింగ్స్") ->
                openScreen(Intent(Settings.ACTION_SOUND_SETTINGS), "Opening sound settings.")

            containsAny(text, "notification settings", "నోటిఫికేషన్ సెట్టింగ్స్") ->
                openScreen(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName), "Opening JARVIS notification settings.")

            containsAny(text, "do not disturb", "dnd", "డూ నాట్ డిస్టర్బ్") ->
                openScreen(Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS), "Opening Do Not Disturb settings. Change the mode there.")

            containsAny(text, "open settings", "settings open", "సెట్టింగ్స్ ఓపెన్") ->
                openScreen(Intent(Settings.ACTION_SETTINGS), "Opening Settings.")

            text.startsWith("search ") || text.startsWith("google ") -> {
                val query = raw.trim().substringAfter(' ').trim()
                if (query.isBlank()) response("Tell me what you want to search for.", ResponseStatus.ERROR)
                else openScreen(Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query), "Searching the web for $query.")
            }

            text.startsWith("open ") -> launchAppByLabel(raw.trim().substringAfter(' ').trim())

            else -> response("I can open apps and system panels, adjust media volume, and set alarms or timers. More controls need Android permissions or additional integration.", ResponseStatus.INFO)
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
