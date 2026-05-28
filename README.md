# Android Weather Planner

Android Weather Planner is a Java Android app that combines saved city management, local user profiles, maps, current weather, and AI-assisted weather insights.

This repository is a private portfolio copy prepared from a team software engineering project. The first cleanup pass removed course progress logs and generated coverage output, documented external dependencies, and added no-key fallback behavior for local demo use.

## Features

- Sign up and sign in with a local SQLite-backed user profile.
- Save and remove cities from a personal city list.
- View city details with current weather data.
- Open an interactive Google Maps view for saved locations.
- Generate weather-themed questions, answers, themes, and images when Gemini keys are configured.
- Use demo weather and fallback AI responses when keys are not configured.

## Tech Stack

- Java
- Android Gradle Plugin
- SQLite
- Google Maps SDK
- OpenWeatherMap API
- Gemini / Google GenAI client libraries
- JUnit, Espresso, and JaCoCo

## Local Setup

Open the project in Android Studio or build from the command line.

```bash
cp local.properties.example local.properties
./gradlew assembleDebug
```

For a no-key demo, leave the values in `local.properties` blank. The app will still build and use mock weather/AI fallbacks where possible.

## External Services

Set these values in `local.properties` for full behavior:

```properties
GEMINI_API_KEY=
OPENWEATHERMAP_API_KEY=
MAPS_API_KEY=
```

- `GEMINI_API_KEY`: powers theme generation, weather questions/answers, and image generation.
- `OPENWEATHERMAP_API_KEY`: powers live current weather.
- `MAPS_API_KEY`: powers Google Maps display.

Do not commit real API keys.

## Verification

Fast local checks:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Instrumented tests require an emulator or device:

```bash
./gradlew connectedDebugAndroidTest
```

## Portfolio Notes

The package namespace remains `edu.uiuc.cs427app` for this initial cleanup to avoid a broad Android refactor. Future polish can rename the application package, modernize dependency versions, and add screenshots from an emulator run.
