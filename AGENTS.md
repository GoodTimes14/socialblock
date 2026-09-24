# AGENTS.md

## Project

Android app that helps users reduce social-media usage through time limits and interruption overlays.
The app overlay should include messages like the ones on a pack of cigarettes, in italian for example:
"I social media provocano dipendenza, smetti subito"
"I social media riducono la tua soglia di attenzione"
"L'abuso dei social media può renderti facilmente manipolabile"
The overlay messages should be semi transparent and non interactable, the interaction of the application that is being overlayed should not be altered by the overlay.

This project has a timer that closes automatically the application after reaching its limit and is set to that specified application
there should be an emergency disable of that timer just in case the user needs to use that application

## Notifications 

There should be push notifications for events like:
- Approaching the limits (5 minute before)
- Daily overall usage stats: on a configurable hour of the day (default: 22:00), the app should sent a notification saying how much the user has used the tracked apps
- Timer limit reached
- Timer emergency exit enabled


## Stack

- Language: Kotlin
- UI: Jetpack Compose with Material 3
- Architecture: MVVM with unidirectional data flow
- Dependency injection: Hilt
- Local database: Room
- Preferences: DataStore
- Async work: Kotlin Coroutines and Flow
- Background tasks: WorkManager
- Usage monitoring: Android `UsageStatsManager`
- Overlays: Android `WindowManager`
- Navigation: Navigation Compose
- Testing: JUnit, MockK, Turbine, and Compose UI tests
- Static analysis: Android Lint, ktlint, and detekt
- Build system: Gradle with Kotlin DSL
- Version control: Git and GitHub

## Commands

Before finishing a change, run:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug ```


## Rules

- Keep changes small and focused.
- Follow the existing architecture and coding style.
- Store user data locally by default.
- Never collect screen contents or browsing data.
- Request permissions only when needed and explain why.
- Keep usage tracking, blocking rules, and UI separate.
- Always provide an emergency bypass.
- Do not add Accessibility Service without explicit approval.
- Add or update tests for changed behavior.
- Never commit secrets, signing keys, or local configuration.
- After any changes, always test with an emulator for verifying that everything is correct and there are no regressions
