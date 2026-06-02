# Android Weather Planner

Android Weather Planner is a Java Android app for saving city watchlists, viewing current weather, opening city maps, and generating AI-assisted weather insights.

This repository is a public portfolio copy of a collaborative software engineering project. My portfolio pass focused on making the app safe to publish, documenting setup and architecture, and preserving a clean interview walkthrough of the Android flow, local persistence, and external API integrations.

## Features

- Local sign-up and sign-in backed by SQLite.
- Per-user saved city lists through a ContentProvider.
- Per-user visual themes generated from natural language prompts when Gemini is configured.
- Weather details powered by OpenWeatherMap when configured, with local demo weather when no key is present.
- Weather insight questions and answers powered by Gemini when configured, with local demo responses when no key is present.
- Weather-aware image generation through Gemini when configured, with a local generated image fallback for no-key demos.
- City map screen powered by Google Maps SDK when configured, with coordinate-only demo mode when no Maps key is present.

## Architecture

The app is a single Android application module using Java, AndroidX AppCompat, SQLite, OkHttp, Gson, Google Maps SDK, and Gemini client libraries.

- `LoginActivity` and `SignUpActivity` manage local account flow and optional theme generation.
- `MainActivity` displays the signed-in user's city list and routes to weather or map views.
- `DetailsActivity` fetches weather data, renders the weather card, and starts AI image generation.
- `WeatherInsightsActivity` displays generated question buttons and answers.
- `UserDatabaseHelper`, `LocationDbHelper`, `LocationProvider`, and `LocationContract` manage local persistence.
- `WeatherApiService`, `ThemeGenerator`, `WeatherInsightsGenerator`, and `WeatherImageGenerator` isolate external API behavior and demo fallbacks.

The Java package, Android namespace, application ID, and ContentProvider authority now use the public portfolio identity `com.weatherplanner.app`.

## Local Setup

Open the project in Android Studio, or configure the command line:

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
GEMINI_API_KEY=
OPENWEATHERMAP_API_KEY=
MAPS_API_KEY=
```

Leaving API keys blank enables demo mode. The app still builds and can be reviewed without live Gemini, OpenWeatherMap, or Google Maps calls.

## Credentials And API Setup

The app needs these local-only values for full live functionality:

```properties
GEMINI_API_KEY=
OPENWEATHERMAP_API_KEY=
MAPS_API_KEY=
```

Only add these to `local.properties`; never commit real values.

`GEMINI_API_KEY`

- Used for theme generation, weather insight questions and answers, and weather-aware city images.
- Blank value enables local theme, Q&A, and image fallbacks.
- Text features use `gemini-2.5-flash-lite` for low-cost, low-latency JSON responses.
- Image generation uses `imagen-4.0-fast-generate-001`, the lowest-cost Imagen 4 variant.
- Use a Gemini API key with access to Gemini text generation and Imagen image generation.
- Keep the key out of git, monitor free-tier usage, and set low quota limits where your provider account allows it.

`OPENWEATHERMAP_API_KEY`

- Used for live current weather data.
- Blank value enables deterministic demo weather.
- For a public mobile demo, use a limited/free key and monitor usage. For production, proxy weather calls through a backend instead of shipping the key in the APK.

`MAPS_API_KEY`

- Used for the interactive Google Maps screen.
- Blank value shows city name, latitude, longitude, and a demo-mode message.
- Enable Maps SDK for Android in Google Cloud.
- Restrict the key to Android apps using package name `com.weatherplanner.app` and your debug or release certificate SHA-1.
- Restrict APIs to Maps SDK for Android.
- The current app does not require Places API, Routes API, Maps JavaScript API, Maps Static API, or Google Maps Platform Geocoding API.

Get the debug SHA-1 with:

```bash
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android
```

Do not commit real keys, signing files, SDK paths, generated APKs, or coverage artifacts.

## Build And Test

Fast checks:

```bash
./gradlew test
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Connected tests require an emulator or device:

```bash
adb devices
./gradlew connectedDebugAndroidTest
```

## Android Studio Emulator Walkthrough

1. Open Android Studio.
2. Select **Open** and choose this repository folder.
3. Let Gradle sync finish. If Android Studio asks for a JDK, choose Java 17.
4. Open `local.properties` and confirm `sdk.dir` points to your Android SDK.
5. Add `GEMINI_API_KEY`, `OPENWEATHERMAP_API_KEY`, and `MAPS_API_KEY` if you want live integrations.
6. Open **Tools > Device Manager**.
7. Create or start an Android Virtual Device. A recent Pixel image on API 34 is a good default.
8. Wait until the emulator appears in the Android Studio device selector.
9. Select the `app` run configuration.
10. Click **Run**.
11. In the emulator, create a local account, sign in, add a city, then open Weather, Weather Insights, and Map.
12. If Maps shows demo mode, rebuild after adding a valid `MAPS_API_KEY`.

## Manual QA Checklist

- Create two local users with different theme prompts.
- Sign in, sign out, and confirm the session clears while accounts persist.
- Add, view, and remove saved cities for each user.
- Open weather details with blank API keys and confirm demo weather plus local image fallback.
- Open weather insights with blank API keys and confirm question buttons and answer text render.
- Open the map screen with blank `MAPS_API_KEY` and confirm coordinates plus demo message render.
- Add live keys and re-check weather, Gemini theme/insight/image generation, and interactive map behavior.

## Portfolio Framing

This app is useful to discuss Android activity flow, local persistence with SQLite and ContentProviders, handling external API configuration safely, graceful no-key demo behavior, and the tradeoffs involved in turning a collaborative prototype into a public portfolio project.
