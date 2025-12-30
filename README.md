# 🚗 TrackMyAuto – Real-Time Vehicle & Driver Tracking

**Reliable Android Tracking App for Vehicle Owners**
Kotlin • Jetpack Compose • Foreground Services • Firebase • MapLibre • WorkManager

TrackMyAuto is a production-ready Android application that enables vehicle owners to **track drivers in real time**, monitor **daily earnings**, and manage **distance-based income (km meter)**.

The app is built with a strong focus on **background reliability**, ensuring continuous tracking even after app kill, device reboot, or aggressive battery optimizations.

---

## ✨ Features

### 🔹 Live Location Tracking

* Real-time GPS tracking using Fused Location Provider
* High-accuracy location updates
* Speed calculation using GPS sensor with fallback logic
* Owner-controlled tracking via Firebase Realtime Database
* No offline storage — **pure live tracking only**

### 🔹 Robust Background Execution

* Foreground location service with persistent notification
* Auto-restart on device reboot (BOOT_COMPLETED)
* WorkManager watchdog to revive killed services
* AlarmManager heartbeat to prevent system suspension
* WakeLock for long-running background stability

### 🔹 Vehicle Money & Meter Management

* Manual **daily earnings** tracking
* **Distance-based earnings** using kilometer (km) meter
* Suitable for taxi, logistics, rental, and fleet use cases

### 🔹 Authentication

* Google Sign-In using Firebase Authentication
* Simple flow — no strict role separation
* Any logged-in user can act as **owner or driver**

### 🔹 Live Map View

* Real-time vehicle visualization
* Powered by **MapLibre**
* Optimized for continuous live updates

---

## 🛠 Tech Stack

* Kotlin
* Jetpack Compose
* Android Foreground Services
* Google Fused Location API
* Firebase Authentication
* Firebase Realtime Database
* WorkManager
* AlarmManager
* MapLibre
* Hilt (Dependency Injection)

---

## 🧩 Architecture Overview

### ▶ LiveLocationService

* Foreground LifecycleService for GPS tracking
* Handles speed calculation and data upload
* Battery-aware with WakeLock + AlarmManager

### ▶ BootReceiver

* Automatically restarts tracking service after device reboot

### ▶ ServiceWatchdogWorker

* Periodic WorkManager task
* Detects and restarts killed services

### ▶ Network & Backend Layer

* Firebase Realtime Database for live tracking state
* Firebase Authentication for secure login

### ▶ UI Layer

* Jetpack Compose UI
* Navigation Compose
* StateFlow-based state management
* Material 3 components

---

## 🔐 Permissions

* ACCESS_FINE_LOCATION
* ACCESS_COARSE_LOCATION
* ACCESS_BACKGROUND_LOCATION
* FOREGROUND_SERVICE
* FOREGROUND_SERVICE_LOCATION
* RECEIVE_BOOT_COMPLETED
* WAKE_LOCK
* REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
* INTERNET
* ACCESS_NETWORK_STATE
* POST_NOTIFICATIONS

These permissions ensure **accurate, uninterrupted tracking** under all system conditions.

---

## 📸 Screenshots
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/1df83887-f728-4e03-8b25-5fc24e1acb22" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/c4c08716-c379-40ab-b756-bc1576c7de06" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/74cf2d97-5021-4dd0-8495-af3b3184eec1" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/69eb714a-f9c0-4b4c-b5e3-e16ecd41b660" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/68027926-dc56-4730-be9d-00c3580eb107" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/2f09fc69-934d-4f88-b212-38e36bd63ab9" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/27245e86-84f5-41e2-8217-52586937e101" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/d9f1adc0-bb90-466b-b0d5-f7ef3f4e749a" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/afa522dd-7ad3-42a9-bfe0-9184bbd815b2" />
<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/b278a301-db1d-4038-8f22-0f410089d3b6" />

---

## 📱 APK Download

*(Add APK link here)*

---

## ⚠️ Important Notes

* No route history is stored
* No offline location caching
* Tracking works strictly in real time
* Designed for reliability over battery savings

---

## 🎯 Current Focus

* Freelance Android development
* Location-based and automation tools
* Building production-ready, background-safe apps

---

## 📫 Contact

**Md Hanzla Tanweer**
📧 Email: [hanzla.code@gmail.com](mailto:hanzla.code@gmail.com)
