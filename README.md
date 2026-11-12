# JARVIS — Native Android Assistant

A Kotlin + Jetpack Compose starter project for a Telugu/English personal assistant, initially targeting the OnePlus 15R and Android 17 (API 37).

## Current starter features

- Native Android app (`com.jarvis.assistant`)
- Dark cyan JARVIS HUD / glass-inspired Compose home screen
- Tap-to-talk using Android `SpeechRecognizer` after runtime microphone permission
- Text-to-speech responses
- Local starter commands: open YouTube, open Settings, open Wi-Fi panel, Bluetooth settings, display settings, sound settings, and web search
- Modular `AIProvider`, `WakeWordEngine`, request/response models and local command router contracts
- GitHub Actions debug APK workflow
- Separate signed-release workflow template

## Important implementation boundaries

This is **Foundation 0.1.0**, not a finished Bixby replacement. The two wake phrases (`Hey, Jarvis` and `Jarvis`) are planned but always-on detection is not enabled in this starter. `WakeWordEngine` is an integration contract only. A production implementation must use Android's supported assistant/hotword APIs where the device exposes them, and must be tested against OnePlus firmware behavior and background microphone restrictions.

The online AI provider is intentionally a placeholder. Do not put a Gemini/API secret in the APK. Add a small authenticated backend and implement `AIProvider` to call it. The backend should enforce rate limits, validate requests, and avoid logging private user content unnecessarily.

The speech recognizer currently prefers `en-IN`. Mixed Telugu-English recognition quality depends on the speech service installed on the phone. Add a language selector and test `te-IN` / `en-IN` behavior on the target device before treating bilingual voice input as complete.

Android restricts direct changes to some system settings. This starter opens the relevant system panel/settings page rather than trying to bypass those restrictions.

## Open in Android Studio

1. Install an Android Studio version that supports Android 17 / API 37 and JDK 17.
2. Install Android SDK Platform 37 and the matching Build Tools from SDK Manager.
3. Extract this ZIP.
4. Open the `JARVIS` folder in Android Studio.
5. Allow Gradle sync and install any prompted SDK components.
6. Connect the OnePlus 15R with USB debugging enabled, or use an emulator.
7. Run the `app` configuration.

This archive includes `gradle-wrapper.properties` but not the binary Gradle wrapper JAR. Android Studio can sync using a configured Gradle installation; to generate a complete wrapper on a machine with Gradle 9.6 installed, run:

```bash
gradle wrapper --gradle-version 9.6.0
```

## Build an APK locally

With Gradle 9.6, JDK 17, and Android SDK 37 installed:

```bash
gradle :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions — debug APK

1. Create a new GitHub repository named `JARVIS`.
2. Upload the contents of this folder to the repository root.
3. Open **Actions** and enable workflows if GitHub asks.
4. Run **Android APK Build** manually, or push a change to `main`.
5. Download `JARVIS-debug-apk` from the completed workflow's Artifacts section.

The workflow installs Gradle 9.6 and uses JDK 17. Android SDK components are resolved by the Android Gradle Plugin on the GitHub runner.

## Signed release workflow

The release workflow expects these GitHub Actions repository secrets:

- `ANDROID_KEYSTORE_BASE64` — base64-encoded release keystore
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Keep the keystore and passwords private. Never commit them. The release workflow is a template and should be tested with a dedicated release keystore before distributing an APK. Back up the keystore securely; losing it can prevent updates signed with the same app identity.

## Planned milestones

1. Validate the starter build and UI on the OnePlus 15R.
2. Add Telugu/English language selection and improve speech recognition handling.
3. Add an authenticated AI backend and Gemini-compatible provider implementation.
4. Add more skills with explicit permission checks and confirmations.
5. Investigate `VoiceInteractionService` / system hotword integration supported by the exact OnePlus firmware. Do not promise always-on wake words until device testing confirms it.
6. Add tests, privacy controls, battery profiling and release signing.

## Project layout

```text
app/src/main/java/com/jarvis/assistant/
  ai/       AI provider contract and safe unconfigured placeholder
  core/     assistant request/response models
  skills/   local command router
  ui/       Compose JARVIS interface
  voice/    tap-to-talk speech controller and wake-word contract
.github/workflows/
  android-build.yml       debug APK build
  android-release.yml     signed release APK template
```
