# Tsunami TV
Local (needs JDK 17 + Gradle 8.10; run `gradle wrapper` once):
  Windows run:    gradlew :composeApp:run
  Windows .exe:   gradlew :composeApp:packageExe        (WiX Toolset 3.x required)
  Portable app:   gradlew :composeApp:createDistributable
  Android APK:    gradlew :composeApp:assembleDebug     (Android SDK required)
No local setup: push this folder to GitHub; .github/workflows/build.yml builds the .exe and .apk (Actions > Artifacts).
Windows playback needs 64-bit VLC 3.x. Enter your M3U or Xtream details on first launch.
