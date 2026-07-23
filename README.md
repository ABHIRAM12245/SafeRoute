<p align="center">
  <img src="docs/Logo.png" alt="RouteGuard Logo" width="180"/>
</p>

# 🛡️ RouteGuard

> **Know Before You Go** — A modern, community-driven road safety and real-time hazard reporting application for Android.

---

## 📌 Project Overview

**RouteGuard** is a native Android application engineered to enhance road safety through crowdsourced hazard intelligence, proximity alerts, and community-verified road reports. Built with Java, Material 3 safety design tokens, and OpenStreetMap (OSMDroid), RouteGuard enables drivers and commuters to report and stay informed about real-time hazards such as accidents, police checks, water logging, road construction, and heavy traffic.

---

## 🎯 Mission

Our mission is to reduce road accidents and travel delays by empowering communities with real-time, peer-verified safety intelligence and automated travel mode hazard detection.

---

## ✨ Key Features

- **🗺️ Interactive OpenStreetMap Navigation**: Smooth map rendering powered by OSMDroid with custom category markers, location centering, and dynamic search radius visualization.
- **🚗 Travel Mode Proximity Alerts**: Background and foreground proximity monitoring that alerts drivers to nearby road hazards in real-time.
- **📢 Crowdsourced Hazard Reporting**: Instant report submission with category selection, location tagging, optional photo attachments (Cloudinary), and duplicate detection.
- **⭐ Community Trust & Verification System**: Multi-user confirmation engine (Still Exists / Not Found) and upvote/downvote scoring to calculate report reliability and trust scores.
- **💬 Real-Time Discussion & Comments**: Interactive discussion threads on reports allowing users to share real-time updates.
- **🔔 Notifications & Activity Center**: In-app activity feed and push notifications (Firebase FCM) grouped by time (Today, Yesterday, Older).
- **🛡️ User Reputation & Achievements**: Gamified reporter levels (Verified Reporter, Contributor, Road Guardian) and reputation badges.
- **📡 Offline Detection & Error Resilience**: Instant offline detection via `SnackbarUtils` with graceful degradation and cached data visibility.

---

## 🏗️ Architecture & Design Pattern

RouteGuard follows clean architecture principles using the **MVVM (Model-View-ViewModel)** design pattern coupled with the Repository pattern for data abstraction:

```
[ UI Layer (Activities / Fragments / BottomSheets) ]
                       │
                       ▼
            [ ViewModel Layer ]
                       │
                       ▼
          [ Repository Layer ]
          ╱         │         ╲
         ▼          ▼          ▼
   [Firestore] [Cloudinary] [Nominatim API]
```

- **UI Layer**: Built using XML layouts with Material 3 safety tokens and ViewBinding.
- **ViewModel Layer**: Manages UI state, LiveData propagation, and background thread execution.
- **Repository Layer**: Encapsulates data sources (Firebase Firestore, Nominatim Service, Cloudinary).

---

## 📁 Project Folder Structure

```
RouteGuardApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/vlabs/routeguard/
│   │   │   │   ├── activities/        # Splash, Home, MyReports
│   │   │   │   ├── adapters/          # RecyclerView Adapters (Comments, Reports, Places, Notifications)
│   │   │   │   ├── api/               # Retrofit Clients & Nominatim Geocoding Services
│   │   │   │   ├── authentication/   # Login & Registration Fragments
│   │   │   │   ├── fragments/        # Bottom Sheet Dialogs (Report Details, Profile, Filters)
│   │   │   │   ├── managers/         # Map, Marker, TravelMode, Trust, Voting, Location Managers
│   │   │   │   ├── models/           # Data Models (RoadReport, User, Vote, Comment, Notification)
│   │   │   │   ├── notifications/    # Notification Activity & Notification Engine
│   │   │   │   ├── receivers/        # BootReceiver for system startup events
│   │   │   │   ├── reports/          # Report Creation Activity & Camera Integration
│   │   │   │   ├── repositories/     # Firestore, Storage, & Search Data Repositories
│   │   │   │   ├── services/         # Firebase Messaging Service (FCM)
│   │   │   │   ├── settings/         # Settings & About Activities
│   │   │   │   ├── utils/            # NetworkUtils, SnackbarUtils, LocationUtils
│   │   │   │   ├── viewmodels/       # HomeViewModel, ReportCreationViewModel, AuthViewModel
│   │   │   │   └── workers/          # TravelMode Periodic WorkManager Workers
│   │   │   ├── res/                  # Layouts, Drawables, Values, Menus, Animations
│   │   │   └── AndroidManifest.xml   # Manifest declaration & permissions
│   │   └── test/                     # Unit Tests (Robolectric & JUnit)
│   ├── build.gradle.kts              # App module build script
│   └── proguard-rules.pro            # ProGuard / R8 rules
├── docs/                             # Release Notes & Project Documentation
├── build.gradle.kts                  # Root Gradle build configuration
└── settings.gradle.kts               # Module settings
```

---

## 🛠️ Technology Stack & Libraries

- **Language**: Java (JDK 17)
- **Min SDK**: API 26 (Android 8.0 Oreo) | **Target SDK**: API 34 (Android 14)
- **UI Framework**: Material Design 3, ViewBinding, ConstraintLayout, SwipeRefreshLayout
- **Map & Geocoding**: OSMDroid 6.1.20, Retrofit 2.9.0 (OpenStreetMap Nominatim API)
- **Backend & Auth**: Firebase BOM 34.15.0 (Firebase Auth, Cloud Firestore, Firebase Cloud Messaging)
- **Image Processing & Cloud**: Glide 4.16.0, Cloudinary REST API
- **Background Tasks**: AndroidX WorkManager 2.9.0, FusedLocationProviderClient

---

## ⚙️ Setup & Configuration

### 1. Firebase Setup
1. Create a project on the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Email/Password Authentication** in Firebase Auth.
3. Create a **Cloud Firestore** database.
4. Download `google-services.json` and place it in the `app/` directory (`app/google-services.json`).

### 2. Nominatim API Setup
RouteGuard uses OpenStreetMap Nominatim API for place search. Ensure custom user-agent identification is set (configured automatically in `HomeActivity`).

---

## 🚀 Build & Installation Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/Vlabs99/RouteGuard.git
cd RouteGuardApp
```

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```
*Output location: `app/build/outputs/apk/debug/app-debug.apk`*

### 3. Build Release APK (Local RC Verification)
```bash
./gradlew assembleRelease
```
*Output location: `app/build/outputs/apk/release/app-release.apk`*

### 4. Build Release Android App Bundle (AAB)
```bash
./gradlew bundleRelease
```
*Output location: `app/build/outputs/bundle/release/app-release.aab`*

---

## 📸 Screenshots Showcase

*(Actual UI screen captures from running RouteGuard application)*

| Home Map & Travel Mode | Report Creation | Report Details & Voting |
| :---: | :---: | :---: |
| ![Home](docs/screenshots/home.png) | ![Report](docs/screenshots/report.png) | ![Details](docs/screenshots/details.png) |

| Notifications Center | User Profile & Trust | My Reports |
| :---: | :---: | :---: |
| ![Notifications](docs/screenshots/notifications.png) | ![Profile](docs/screenshots/profile.png) | ![My Reports](docs/screenshots/my_reports.png) |

---

## ⚠️ Known Limitations

- **Cloudinary Integration**: Uses Cloudinary REST endpoints for report photo attachments instead of Firebase Storage.
- **Offline Mode**: Report creation requires an active internet connection; offline queueing is not supported in v1.0.
- **Community Dependency**: Accuracy of report trust scores depends on user confirmations and upvotes.
- **Map Provider**: Powered by OpenStreetMap (OSMDroid) instead of Google Maps API.

---

## 🔮 Future Roadmap

- [ ] Offline report draft queueing with auto-sync upon reconnecting.
- [ ] Voice-assisted hazard reporting hands-free mode.
- [ ] Turn-by-turn navigation overlay integrated with hazard warnings.
- [ ] AI-powered hazard prediction.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more details.

---

## 👨‍💻 Developer Information

**Vishwarajsinh Chudasama**  
*Software Developer & MCA Student*  
- **GitHub**: [github.com](https://github.com/Vlabs99)  
- **LinkedIn**: [linkedin.com](https://linkedin.com)  

---
*RouteGuard • Safer Roads Together • © 2026*
