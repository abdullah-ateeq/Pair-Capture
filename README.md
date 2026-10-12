# Pair Capture 🧩

**Pair Capture** is a modern, sleek memory tile-matching game for Android built from the ground up with **Jetpack Compose** and **Material Design 3**. Challenge yourself against an intelligent CPU opponent on a 4x4 grid, track detailed gameplay statistics, and customize your experience with dynamic themes, haptics, and sound effects.

---

## 📸 Overview & Features

### 🎮 Gameplay Highlights
* **4x4 Grid Memory Challenge**: Uncover pairs from 8 distinct geometric symbols (*Circle, Triangle, Square, Star, Diamond, Hexagon, Moon, Bolt*).
* **AI Opponent**: Play against a smart CPU opponent with three distinct difficulty levels:
  * 🟢 **Easy**: Short memory retention; relies mostly on random exploration.
  * 🟡 **Normal**: Remembers revealed tiles accurately during the match.
  * 🔴 **Hard**: Sharp memory; immediately claims known pairs and avoids redundant picks.
* **Streak & Pair Runs**: Build pair streaks for multiplier momentum and extra feedback.
* **Detailed Match Statistics**: Tracks total games, win rate, best streak, average pairs captured per game, and best score across sessions.

### 🎨 UI & User Experience
* **100% Jetpack Compose & Material 3**: Beautiful UI with smooth screen transitions and responsive components.
* **Adaptive Layouts**: Optimized experience across mobile phones, foldables, and landscape tablet layouts.
* **3D Animations & Feedback**: Fluid tile flips, tile pulse animations on matches, tilt/shake on mismatches, customizable sound effects, and haptic feedback.
* **Appearance Customization**: Dark/Light theme switching, Material You Dynamic Color support (Android 12+), turn counter toggles, and restart confirmation prompts.

---

## 🏗️ Architecture & Technical Highlights

Pair Capture adheres to modern Android architecture best practices:

* **Pure Functional Game Engine (`PairCaptureGameEngine`)**: Statemachine logic isolated from UI and ViewModel, enabling 100% deterministic testing.
* **Hidden Information Security (`CpuVisibleState`)**: The CPU AI strategy receives only a sanitized view of the board (revealed tiles + memory), guaranteeing no "cheating" by inspecting underlying tile data.
* **Reactive State Management**: Driven by `StateFlow` and `PairCaptureViewModel` following the Unidirectional Data Flow (UDF) pattern.
* **Local Persistence**: Integrated with `DataStore` / `PreferenceStore` for persisting user preferences and lifetime game statistics.
* **Unit Test Coverage**: Includes 55+ unit tests validating game engine rules, CPU AI memory lifetime, state safety, and persistence flows.

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio** (Ladybug or newer recommended)
* **JDK 11** or higher
* **Android Device or Emulator** running API 24 (Android 7.0) or higher (Target SDK 37)

### Build & Run
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/Pair-Capture.git
   cd Pair-Capture
   ```

2. **Open in Android Studio**:
   Open the project folder in Android Studio and let Gradle sync dependencies.

3. **Run the App**:
   Select your target device/emulator and press **Run** (`Shift + F10`), or execute via command line:
   ```bash
   ./gradlew installDebug
   ```

4. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 🛠️ Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material 3](https://m3.material.io/)
* **Navigation**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
* **State Management**: [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel), [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
* **Data Storage**: Preferences DataStore
* **Testing**: JUnit 4, Kotlinx Coroutines Test, Compose UI Test

---

## 📄 License

```text
Copyright 2025 Abdullah Ateeq

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied.
See the License for the specific language governing permissions and limitations
under the License.
```
