# Zero Touch Budget

Zero Touch Budget is an Android budgeting app that reduces manual expense tracking as much as possible.
It combines bank notification parsing, receipt OCR, manual fallback, background auto-scan, and a home screen widget so users can see their daily budget at a glance.

## Project Overview

The app is designed around a local-first budgeting workflow:

- capture spending automatically when possible,
- fall back to manual entry when needed,
- keep daily totals and summaries on-device,
- and surface the most important number on the widget and home screen.

This repository also includes a flow document that explains the full wireframe and user journey:

- [Wireframe and flows](docs/WIREFRAME.md)

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Hilt
- Room
- WorkManager
- DataStore
- Glance App Widget
- ML Kit Text Recognition
- Gemini API
- AndroidX libraries such as Lifecycle, Activity Compose, ExifInterface, and DocumentFile
- JUnit, MockK, and Turbine

## Features

- Daily budget dashboard with remaining budget, spent amount, and progress state
- Manual add, edit, and delete transaction flow
- Receipt scanning from the gallery
- AI-based receipt extraction using Gemini
- Bank notification auto-tracking for supported banking apps
- Auto-scan scheduler for screenshots, camera images, or a custom folder
- Home screen widget that mirrors the daily summary
- Settings screen for budget, auto-scan, and notification access
- Local storage for transactions and daily summaries

## Main Flows

1. Open the app and view today's budget summary on the home screen.
2. Add an expense manually or scan a receipt from the gallery.
3. Let the app parse bank notifications automatically in the background.
4. Adjust budget and auto-scan settings from the settings screen.
5. Check the home screen widget for a quick overview without opening the app.

For the detailed screen-by-screen flow, see [docs/WIREFRAME.md](docs/WIREFRAME.md).

## Setup

1. Open the project in Android Studio.
2. Use JDK 17.
3. Add your Gemini API key in `gradle.properties`:

   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

4. Sync Gradle and run the app on a device or emulator.

## Permissions

- `INTERNET` for Gemini and network access
- `RECEIVE_BOOT_COMPLETED` for background rescheduling
- `READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE` for image access
- `READ_MEDIA_VISUAL_USER_SELECTED` for newer Android photo access
- Notification listener permission for bank notification tracking

## Repository Structure

- `app/` Android application source code
- `docs/` product and flow documentation
- `docs/WIREFRAME.md` full app flow and screen map
- `docs/PRD-zero-touch-daily-budgeting-app.md` product requirements

## Notes

- Package name: `com.example.zerotouchbudget`
- Minimum SDK: 26
- Main entry point: `MainActivity`
- The widget updates after transaction changes and settings changes
- Bank notification tracking must be enabled manually in system settings

## License

No license has been specified yet.
