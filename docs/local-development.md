# Local Development Cheatsheet

## SDK And Config

Use Android Studio or set `sdk.dir` in an ignored `local.properties` file:

```properties
sdk.dir=/path/to/Android/sdk
GEMINI_API_KEY=
OPENWEATHERMAP_API_KEY=
MAPS_API_KEY=
```

Blank API keys are valid for demo mode. Add real keys only to your local file.

## Common Commands

```bash
./gradlew test
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew clean
```

Connected checks:

```bash
adb devices
./gradlew connectedDebugAndroidTest
```

If no device appears in `adb devices`, start an emulator from Android Studio before running connected tests.

## API Notes

- Gemini powers dynamic themes, weather Q&A, and generated weather images.
- OpenWeatherMap powers live current weather.
- Google Maps powers the interactive map through Maps SDK for Android.
- No-key demo mode should still support sign-in, city lists, weather details, insights, generated local image fallback, and coordinate-only map fallback.

## Credentials For Full Functionality

Add these values to `local.properties`:

```properties
GEMINI_API_KEY=
OPENWEATHERMAP_API_KEY=
MAPS_API_KEY=
```

- `GEMINI_API_KEY`: needs access to Gemini text generation and Imagen image generation. Text calls use `gemini-2.5-flash-lite`; image calls use `imagen-4.0-fast-generate-001`.
- `OPENWEATHERMAP_API_KEY`: needs access to current weather data.
- `MAPS_API_KEY`: enable only Maps SDK for Android and restrict the key to package `com.weatherplanner.app` plus the debug or release SHA-1.

Get the debug SHA-1:

```bash
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android
```

## Android Studio Emulator Steps

1. Open this repo in Android Studio.
2. Wait for Gradle sync to complete.
3. Use Java 17 if Android Studio prompts for a Gradle JDK.
4. Confirm `local.properties` has a valid `sdk.dir`.
5. Add API keys if you want live integrations.
6. Open **Tools > Device Manager**.
7. Create or start an emulator, preferably a Pixel device on API 34.
8. Select the emulator in the device selector.
9. Select the `app` run configuration.
10. Press **Run**.
11. Create a local account in the app, sign in, add a city, and test Weather, Weather Insights, and Map.

## Troubleshooting

- If Gradle cannot find the SDK, update `sdk.dir` in `local.properties`.
- If Gradle or lint fails under a very new JDK, run with a Java 17 toolchain.
- If Maps shows the demo message, `MAPS_API_KEY` is blank or unavailable at build time.
- If live weather fails, confirm the OpenWeatherMap key is active and the device has network access.
- If Gemini calls fail, confirm the key is active and provider quota has not been exhausted.

## Hygiene Checks

Run searches against tracked files before publishing:

```bash
git ls-files -z | xargs -0 rg -n "AIza|sk-[A-Za-z0-9]|absolute/path|private-domain"
git status --ignored --short
```
