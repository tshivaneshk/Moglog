# Moglog 📺

> A modern, sleek, and intuitive general show tracking application built with **Jetpack Compose**, **Material 3**, and **Firebase**.

<p align="center">
  <img src="Logo.png" alt="Moglog Logo" width="160" />
</p>

---

## 🌟 Overview

**Moglog** is an all-in-one show tracking app designed for TV shows, web series, dramas, and anime enthusiasts. Keep track of what you're currently watching, plan upcoming titles, manage completed shows, and sync your watch history securely across devices.

With a focused Emerald & Charcoal aesthetic, smooth 120Hz display optimization, and offline-first capabilities, Moglog delivers a fast, responsive, and distraction-free viewing tracker.

---

## ✨ Key Features

- **Comprehensive Show Tracking**:
  - Categorize shows seamlessly into **Watching**, **Planned**, and **Done** states.
  - Search and discover TV shows and series with comprehensive metadata, posters, and details powered by TMDB.
  - Custom notes and status management for every title on your watchlist.

- **Offline-First Resilience**:
  - Full local persistence with **Room Database** (SQLite).
  - Use the app completely offline—browse, add, edit, or filter your shows without an internet connection.
  - Graceful network timeout handling with contextual toasts when offline.

- **Cloud Sync & Multi-Device Access**:
  - **Firebase Authentication** supporting Google Sign-In and anonymous guest mode.
  - Cloud backup and synchronization via **Cloud Firestore**.
  - Flexible sync options: Automatic sync or configurable **Manual Only** sync frequency.
  - Guest data preservation: Signing in or switching accounts safely preserves your locally tracked shows.

- **Modern & Fluid UI / UX**:
  - Crafted entirely with **Jetpack Compose** following modern Android architectural standards.
  - Distinctive **Emerald & Charcoal** dark palette (60-30-10 design rule).
  - Translucent floating action dock and refined iOS-style bottom controls.
  - Complete responsiveness with adaptive navigation across portrait and landscape orientations.

- **Privacy & Account Control**:
  - Account deletion support directly from the app (wipes cloud document records and auth credentials).
  - Option to back up and restore your TMDB API keys securely.

---

## 🛠️ Architecture & Tech Stack

- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture pattern
- **Asynchronous & Concurrency**: Kotlin Coroutines & `StateFlow`
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/)
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) (memory & disk caching)
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) + OkHttp
- **Cloud Backend**:
  - Firebase Authentication (Google Sign-In)
  - Firebase Cloud Firestore (NoSQL cloud sync)

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug / Hedgehog or newer
- **JDK**: Version 17 or higher
- **Android SDK**: Min SDK 24, Target SDK 35

### Setup Instructions

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/tshivaneshk/Moglog.git
   cd Moglog
   ```

2. **Configure Firebase**:
   - Create a project on the [Firebase Console](https://console.firebase.google.com/).
   - Enable **Authentication** (Google Sign-In Provider) and **Cloud Firestore**.
   - Download your `google-services.json` file.
   - Place `google-services.json` in the `app/` directory (see [`app/google-services.json.example`](app/google-services.json.example)).

3. **TMDB API Key (Optional)**:
   - Obtain a free API key from [The Movie Database (TMDB)](https://www.themoviedb.org/).
   - Add your API key directly inside the app's **Settings** screen or sync it to your cloud profile.

4. **Build & Run**:
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Run on an emulator or a physical device:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 📦 Releases

Download the latest ready-to-install APK from the [Releases](https://github.com/tshivaneshk/Moglog/releases) section.

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
