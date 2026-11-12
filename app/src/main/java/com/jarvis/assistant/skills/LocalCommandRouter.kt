package com.jarvis.assistant.skills

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.jarvis.assistant.core.AssistantResponse
import com.jarvis.assistant.core.ResponseStatus
import java.util.Locale

/** Safe starter skills: launch apps and open relevant Android settings panels. */
class LocalCommandRouter(private val context: Context) {
    fun execute(raw: String): AssistantResponse {
        val text = raw.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return AssistantResponse("I didn't catch that. Please try again.", ResponseStatus.ERROR)

        return when {
            containsAny(text, "open youtube", "youtube open", "యూట్యూబ్ ఓపెన్", "youtube") ->
                launchPackage("com.google.android.youtube", "YouTube")

            containsAny(text, "wifi", "wi-fi", "వైఫై", "వై ఫై") ->
                openSettings(Intent(Settings.Panel.ACTION_WIFI), "Opening Wi-Fi controls. Please make the change there.")

            containsAny(text, "bluetooth", "బ్లూటూత్") ->
                openSettings(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Opening Bluetooth settings.")

            containsAny(text, "brightness", "display settings", "బ్రైట్‌నెస్") ->
                openSettings(Intent(Settings.ACTION_DISPLAY_SETTINGS), "Opening display settings.")

            containsAny(text, "volume", "sound settings", "వాల్యూమ్") ->
                openSettings(Intent(Settings.ACTION_SOUND_SETTINGS), "Opening sound settings.")

            containsAny(text, "open settings", "settings open", "సెట్టింగ్స్ ఓపెన్") ->
                openSettings(Intent(Settings.ACTION_SETTINGS), "Opening Settings.")

            text.startsWith("search ") -> {
                val query = raw.substringAfter("search ").trim()
                val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query)
                openSettings(intent, "Searching the web for $query.")
            }

            else -> AssistantResponse(
                "I can handle basic app and settings commands right now. Online AI and more skills will be added in the next development stage.",
                ResponseStatus.INFO
            )
        }
    }

    private fun launchPackage(packageName: String, label: String): AssistantResponse {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return AssistantResponse("$label is not installed or its launch activity is unavailable.", ResponseStatus.ERROR)
        return openSettings(launch, "Opening $label.")
    }

    private fun openSettings(intent: Intent, message: String): AssistantResponse = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        AssistantResponse(message, ResponseStatus.SUCCESS)
    } catch (_: Exception) {
        AssistantResponse("I couldn't open that screen on this device.", ResponseStatus.ERROR)
    }

    private fun containsAny(text: String, vararg terms: String) = terms.any(text::contains)
}
