
# MathAppLock 🧮📱

An intelligent Android application locker designed to boost focus and productivity by intercepting targeted app launches with procedurally generated Grade 8 math puzzles. Built using modern Android architecture components, Jetpack Compose, and a low-overhead system monitoring service.

## 🚀 Key Features
- **UsageStatsManager Integration:** Employs a robust `Foreground Service` architecture using Android usage events for reliable, battery-friendly foreground tracking.
- **Dynamic Math Engine:** Procedurally generates challenges spanning Linear Equations, Exponents, Perfect Squares, and Geometry.
- **Smart Session Management:** Features a passive 30-minute timestamp comparison window that minimizes processor wakeups and resets upon device lock (`Intent.ACTION_SCREEN_OFF`).
- **Graceful Navigation Overlay:** Custom `WindowManager` fullscreen overlay (`TYPE_APPLICATION_OVERLAY`) that returns users safely to the home launcher when pressing the Back key.
- **Unlock Verification Challenge:** Prevents disabling protection directly from the settings interface by requiring a verified answer before unchecking protected apps.
- **Package Visibility Compliance:** Configured with manifest queries to index both third-party apps and core system packages.
- **Localization:** Out-of-the-box multilingual support with resources in English and Malayalam.

## 🛠️ Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose (App Selection UI) & Native XML (WindowManager Overlay)
- **Android Architecture:** Foreground Service, UsageStatsManager, WindowManager, SharedPreferences, BroadcastReceiver

## 📦 Build & Run
1. Clone the repository:
   ```bash
   git clone [https://github.com/fyszyn/MathAppLock.git](https://github.com/fyszyn/MathAppLock.git)
