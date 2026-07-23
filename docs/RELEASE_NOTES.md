<p align="center">
  <img src="Logo.png" alt="RouteGuard Logo" width="160"/>
</p>

# RouteGuard v1.0 Release Notes

**Release Date**: July 22, 2026  
**Build Version**: 1.0 (Version Code: 1)  
**Package Name**: `com.vlabs.routeguard`  
**Target SDK**: API 34 (Android 14)  
**Min SDK**: API 26 (Android 8.0)  

---

## 🌟 Highlights

- **Material 3 Safety Design System**: Curated color palette (`rg_primary`, `rg_secondary`, `rg_tertiary`) with high contrast outdoor readability, custom bottom sheets, and responsive cards.
- **OpenStreetMap & Geocoding**: High-performance map rendering via OSMDroid with custom category markers, search debouncing, and Nominatim geocoding.
- **Community Hazard Reporting**: Instant crowdsourced report creation across 7 road hazard categories (Police Checking, Accident, Heavy Traffic, Road Construction, Animal on Road, Water Logging, Other) with optional Cloudinary photo uploads.
- **Travel Mode Proximity Engine**: Intelligent background and foreground proximity monitoring that alerts commuters when approaching hazards.
- **Trust & Verification Score**: Dynamic score calculation based on multi-user confirmations (Still Exists / Not Found), user reputation levels, and community upvotes/downvotes.
- **Notifications & Discussion Feed**: In-app notification center with relative timestamps, unread tracking, and real-time comment threads per report.
- **Offline & Low Network Resilience**: Immediate network connection status monitoring via `SnackbarUtils` preventing application crashes during network drops.
- **Firebase Authentication**: Secure user session handling with email/password login and registration.

---

## 🔧 Improvements & Optimizations

- **Layout Performance**: Removed redundant layout measure passes by setting `android:baselineAligned="false"` on horizontal containers and setting weighted view heights to `0dp`.
- **Memory & Lifecycle Safety**: Guarded all asynchronous handler delays against destroyed activity states (`SplashActivity`) and detached map listeners cleanly.
- **Resource Cleanup**: Purged non-standard `.zip` archives and unused drawables, strings, and color tokens.
- **Locale Reliability**: Standardized string lowercasing using `Locale.US` across error handlers to avoid locale-specific bugs.

---

## ⚠️ Known Issues & Limitations

- **Cloudinary Storage**: Photos use Cloudinary REST service instead of Firebase Storage.
- **Offline Submission**: Creating reports requires an active network connection.
- **Community Dependency**: Accuracy depends on user participation in confirming or disputing reports.

---

## 🔮 Future Plans (Post-v1.0)

- Offline report draft queueing with background sync.
- Voice-activated hands-free hazard reporting while driving.
- Turn-by-turn route navigation integration.
- Full dark mode theme support.
