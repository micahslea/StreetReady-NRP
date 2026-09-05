# Build the StreetReady NRP APK

## Android Studio
1. Open this folder in Android Studio.
2. Let Gradle sync complete.
3. Select **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Command line
Requirements: JDK 17 and Android SDK 35.

Linux/macOS:
```bash
./gradlew clean assembleDebug
```

If a system Gradle 8.9 is installed:
```bash
gradle clean assembleDebug
```

## GitHub Actions
The included `.github/workflows/android-build.yml` builds the APK and uploads it as the artifact `StreetReady-NRP-v3-debug-apk`.
