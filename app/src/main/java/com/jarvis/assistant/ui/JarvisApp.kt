package com.jarvis.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jarvis.assistant.core.AssistantResponse
import com.jarvis.assistant.skills.LocalCommandRouter
import com.jarvis.assistant.voice.SpeechController
import java.util.Locale

private val Midnight = Color(0xFF070B13)
private val Panel = Color(0xFF111C2B)
private val Cyan = Color(0xFF65DFFF)
private val Muted = Color(0xFF9BB0C5)

@Composable
fun JarvisApp() {
    val context = LocalContext.current
    val router = remember { LocalCommandRouter(context) }
    val speech = remember { SpeechController(context) }
    var listening by remember { mutableStateOf(false) }
    var speechLanguage by remember { mutableStateOf("en-IN") }
    var heardText by remember { mutableStateOf("Say a command or tap the microphone") }
    var response by remember { mutableStateOf("Hello. I'm JARVIS. Your assistant foundation is ready.") }
    var status by remember { mutableStateOf("READY · TAP TO SPEAK") }
    var ttsReady by remember { mutableStateOf(false) }
    var showPermissionSetup by remember { mutableStateOf(false) }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val writeSettingsGranted = Settings.System.canWrite(context)
    val notificationAccessGranted = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        ?.contains(context.packageName) == true
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val dndGranted = notificationManager.isNotificationPolicyAccessGranted

    val runtimePermissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val denied = result.filterValues { !it }.keys
        response = if (denied.isEmpty()) "Microphone and camera permissions granted." else "Some permissions were not granted. You can enable them in Android App Settings."
        status = if (denied.isEmpty()) "PERMISSIONS READY" else "PERMISSIONS INCOMPLETE"
        permissionRefresh++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("jarvis_setup", 0)
        if (!prefs.getBoolean("permission_intro_shown", false)) {
            showPermissionSetup = true
            prefs.edit().putBoolean("permission_intro_shown", true).apply()
        }
    }

    fun openPermissionPage(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            response = "Could not open that permission page. Please use Android Settings > Apps > JARVIS > Permissions."
            status = "SETTINGS UNAVAILABLE"
        }
    }

    val tts = remember {
        TextToSpeech(context) { result -> ttsReady = result == TextToSpeech.SUCCESS }
    }

    DisposableEffect(Unit) {
        onDispose {
            speech.stop()
            tts.stop()
            tts.shutdown()
        }
    }

    fun handleCommand(command: String) {
        heardText = command
        val result: AssistantResponse = router.execute(command)
        response = result.text
        status = if (result.status.name == "SUCCESS") "ACTION COMPLETED" else "RESPONSE READY"
        if (ttsReady) {
            tts.language = Locale.forLanguageTag(speechLanguage)
            tts.speak(result.text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-response")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            listening = true
            status = "LISTENING…"
            speech.listen(
                languageTag = speechLanguage,
                onResult = { phrase -> listening = false; handleCommand(phrase) },
                onFailure = { message -> listening = false; response = message; status = "VOICE ERROR" }
            )
        } else {
            response = "Microphone permission is needed for voice commands. You can still use the text command field in a later build."
            status = "MICROPHONE PERMISSION NEEDED"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Midnight, surface = Panel)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101E30), Midnight, Color(0xFF05080E))))) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(painter = painterResource(com.jarvis.assistant.R.drawable.ic_jarvis_logo), contentDescription = "JARVIS logo", modifier = Modifier.size(48.dp))
                        Column {
                            Text("J.A.R.V.I.S", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            Text("PERSONAL AI ASSISTANT", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                        }
                    }
                    Icon(Icons.Default.Settings, contentDescription = "Permission settings", tint = Cyan,
                        modifier = Modifier.size(26.dp).clickable { showPermissionSetup = true })
                }
                Spacer(Modifier.height(38.dp))
                Text(status, color = Cyan, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(24.dp))
                VoiceOrb(listening)
                Spacer(Modifier.height(18.dp))
                Text(if (listening) "I'm listening…" else "At your command", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Medium)
                Text("Hey, Jarvis  ·  Jarvis", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = speechLanguage == "en-IN", onClick = { speechLanguage = "en-IN" }, label = { Text("English") })
                    FilterChip(selected = speechLanguage == "te-IN", onClick = { speechLanguage = "te-IN" }, label = { Text("తెలుగు") })
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            listening = true
                            status = "LISTENING…"
                            speech.listen(
                                languageTag = speechLanguage,
                                onResult = { phrase -> listening = false; handleCommand(phrase) },
                                onFailure = { message -> listening = false; response = message; status = "VOICE ERROR" }
                            )
                        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Midnight),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 26.dp, vertical = 15.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text(if (listening) "LISTENING" else "TALK TO JARVIS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(30.dp))
                GlassCard {
                    Text("YOU SAID", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(heardText, color = Color.White, fontSize = 15.sp)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = .10f))
                    Spacer(Modifier.height(14.dp))
                    Text("JARVIS", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(response, color = Color(0xFFE4EDF7), fontSize = 14.sp, lineHeight = 21.sp)
                }
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick = { showPermissionSetup = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("PERMISSION CENTER  ·  ${listOf(micGranted, cameraGranted, writeSettingsGranted, notificationAccessGranted, dndGranted).count { it }}/5 READY")
                }
                Spacer(Modifier.height(14.dp))
                Text("QUICK COMMANDS", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("YouTube", "open youtube", Icons.Default.PlayArrow, ::handleCommand, Modifier.weight(1f))
                    QuickAction("Wi-Fi", "wifi", Icons.Default.Wifi, ::handleCommand, Modifier.weight(1f))
                    QuickAction("Settings", "open settings", Icons.Default.Settings, ::handleCommand, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Flashlight", "flashlight on", Icons.Default.PlayArrow, ::handleCommand, Modifier.weight(1f))
                    QuickAction("Read alerts", "read notifications", Icons.Default.History, ::handleCommand, Modifier.weight(1f))
                    QuickAction("Notif access", "enable notification access", Icons.Default.Settings, ::handleCommand, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Brightness access", "allow brightness", Icons.Default.Settings, ::handleCommand, Modifier.weight(1f))
                    QuickAction("DND access", "allow do not disturb", Icons.Default.Settings, ::handleCommand, Modifier.weight(1f))
                }
                Spacer(Modifier.height(22.dp))
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Cyan)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("WAKE WORD STATUS", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Not configured yet · Tap-to-talk is ready", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("JARVIS  ·  ONEPLUS EDITION", color = Muted.copy(alpha = .65f), fontSize = 9.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            }

            if (showPermissionSetup) {
                AlertDialog(
                    onDismissRequest = { showPermissionSetup = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painter = painterResource(com.jarvis.assistant.R.drawable.ic_jarvis_logo), contentDescription = null, modifier = Modifier.size(38.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("JARVIS PERMISSIONS", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Enable only the access you want. Android will show its own system permission screen for each protected feature.", color = Muted, fontSize = 12.sp)
                            PermissionStatusRow("Microphone · voice commands", micGranted)
                            PermissionStatusRow("Camera · flashlight control", cameraGranted)
                            Button(onClick = {
                                runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                            }, modifier = Modifier.fillMaxWidth()) { Text("REQUEST MICROPHONE + CAMERA") }

                            HorizontalDivider(color = Color.White.copy(alpha = .12f))
                            PermissionStatusRow("Modify system settings · brightness", writeSettingsGranted)
                            OutlinedButton(onClick = {
                                openPermissionPage(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(Uri.parse("package:" + context.packageName)))
                            }, modifier = Modifier.fillMaxWidth()) { Text("OPEN BRIGHTNESS ACCESS") }

                            HorizontalDivider(color = Color.White.copy(alpha = .12f))
                            PermissionStatusRow("Notification access · read recent alerts", notificationAccessGranted)
                            OutlinedButton(onClick = {
                                openPermissionPage(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }, modifier = Modifier.fillMaxWidth()) { Text("OPEN NOTIFICATION ACCESS") }

                            HorizontalDivider(color = Color.White.copy(alpha = .12f))
                            PermissionStatusRow("Do Not Disturb access", dndGranted)
                            OutlinedButton(onClick = {
                                openPermissionPage(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                            }, modifier = Modifier.fillMaxWidth()) { Text("OPEN DND ACCESS") }

                            Text("Internet access is granted automatically by Android. Calls and SMS use the system dialer or messaging app and do not need direct call/SMS permissions. Wake-word background microphone access will be requested only when that feature is implemented.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPermissionSetup = false }) { Text("DONE") }
                    },
                    containerColor = Panel,
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(label: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 12.sp)
        }
        Text(if (granted) "GRANTED" else "NOT GRANTED", color = if (granted) Color(0xFF7CE8B2) else Color(0xFFFFC46B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VoiceOrb(listening: Boolean) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.12f else 1.045f,
        animationSpec = infiniteRepeatable(tween(if (listening) 700 else 1800), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(Modifier.size(190.dp).scale(pulse), contentAlignment = Alignment.Center) {
        Box(Modifier.size(178.dp).border(1.dp, Cyan.copy(alpha = .25f), CircleShape).padding(12.dp).border(1.dp, Cyan.copy(alpha = .35f), CircleShape))
        Box(Modifier.size(112.dp).background(Brush.radialGradient(listOf(Color(0xFFB9F5FF), Color(0xFF168AB9), Color(0xFF09223C))), CircleShape).border(2.dp, Cyan.copy(alpha = .8f), CircleShape), contentAlignment = Alignment.Center) {
            Text("J", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color.White.copy(alpha = .10f), Color.White.copy(alpha = .035f))), RoundedCornerShape(22.dp))
            .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(22.dp)).padding(18.dp),
        content = content
    )
}

@Composable
private fun QuickAction(label: String, command: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Panel.copy(alpha = .8f), RoundedCornerShape(17.dp)).border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(17.dp))
            .clickable { onClick(command) }.padding(vertical = 15.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Cyan)
        Spacer(Modifier.height(7.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
