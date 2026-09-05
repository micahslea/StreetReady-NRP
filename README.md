# StreetReady NRP — Paramedic Academy v3 (Native Android)

This version is a native Android/Jetpack Compose project. It is not a WebView wrapper.

## What is in v3
- 53-chapter curriculum path
- 636 teaching cards (12 per chapter)
- Native illustrations drawn with Jetpack Compose Canvas
- Native Android TextToSpeech narration
- Foreground narration service so lesson audio can continue with the screen off
- Notification control to stop narration
- Offline-first bundled course content (`app/src/main/assets/course.json`)
- Persistent chapter/card progress through SharedPreferences
- 40 Clinical Judgment cases using a six-step reasoning path
- 48 ECG/rhythm/conduction/ischemia teaching patterns
- ECG learn and identification modes
- 424 original reinforcement questions
- Teaching-first navigation: Learn, Listen, Cases, ECG, Practice

## Important clinical note
StreetReady is an educational study tool, not a treatment protocol. Medication choice, dose, procedure indications, destination rules, and scope vary by jurisdiction and medical direction. Verify treatment details against your current local protocol and current authoritative guidelines.

## Build in Android Studio
1. Install the current stable Android Studio.
2. Open the `StreetReady-NRP-Android-v3` folder as a project.
3. Allow Gradle sync to download Android/Compose dependencies.
4. Use an Android 8.0+ device/emulator (minSdk 26).
5. Run the `app` configuration.
6. For an installable APK: Build > Build App Bundle(s) / APK(s) > Build APK(s).

The project targets Android API 35 and is designed for a modern Samsung/Android phone form factor.

## Why there is no APK in this package
The current artifact environment does not contain the Android SDK/Gradle Android build toolchain, so I could create and validate the native Android Studio source project but could not truthfully claim to have compiled or signed an APK here.

## Content philosophy
The course is original, concept-driven teaching based on the publicly described 9th-edition organization of *Nancy Caroline's Emergency Care in the Streets* and the public National Registry paramedic framework. It does not reproduce proprietary textbook chapters, figures, or commercial question-bank items.
