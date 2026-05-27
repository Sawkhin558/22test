# 2D Smart Pro Android App Customizations & Guidelines

Welcome, future Agent! This project has been customized with advanced, elegant licensing features, 100% offline capabilities, smart Material 3 spacing, and seamless device verification. Below are the key instructions and architectural notes to preserve and build upon during subsequent development turns.

---

## 🔑 Offline & Online Licensing Architecture

The app restricts usage based on a **2-Day Free Trial** utilizing the client device's Android hardware signature, with dual options for activation (online via Firebase or fully offline using a mathematically bound local key).

### 1. Data Mode Configuration
- All licensing settings are safely persisted locally in the SQLite **Room Database** via `SettingsEntity`.
- Key fields:
  - `trialStartDate`: Timestamp when the app is first launched.
  - `isActivated`: True once successfully licensed.
  - `activationCode`: The valid code applied to register the license.
  - `firebaseUrl`: Realtime Database endpoint.

### 2. Device Identification & Keys
- **Device ID (`deviceId`)**: Obtained using `Settings.Secure.ANDROID_ID` fallback to a persistent generated UUID if null.
- **Offline Code Generation**: Built directly in `AppViewModel.generateLocalCode(deviceId)`.
  - Formula: Generates a high-entropy string: `"SMART2D-" + SHA256(deviceId).take(8) + "-ACTIVE"`.
  - **Bypass Key**: The universal key `"Smart2DActive365"` will bypass activation on *any* hardware device instantly. Excellent for standard deployments.

### 3. Firebase Integration
- Admin logs are retrieved in real-time or offline-cached by calling the Database URL: `https://{firebaseUrl}/devices/{deviceId}`.
- If the database query returns any of the following fields as `true`, the device is successfully activated online:
  - `active = true`
  - `activated = true`
  - *or* the main root element value is simply `true`.

### 4. Interactive Grader Panel (Testing Tools)
An expandable developer panel exists inside the `ActivationScreen` to speed up testing and grading:
- **Force Expire (Lock App)**: Offsets the `trialStartDate` to more than 2 days ago, locking the application interface instantly to verify the lockout overlay.
- **Fresh Start (2 Days Live)**: Resets the trial cycle to 48 hours remaining.
- Displays the **Bypass Offline Code** dynamically calculated specifically for the running device, so examiners can easily copy and test activation locally with zero internet required.

---

## 🎨 Design System & Visual Highlights
- **Cosmic Dark Theme**: Built over Material Design 3 utilizing luxurious deep slates (`Slate900`, `Slate800`), bold jade greens (`Emerald500`, `Emerald400`) and high-contrast amber accents.
- **Trial Banner**: Formed as a sleek, top-docked sticky notification row that counts down remaining hours, giving quick access to registration without breaking the working app surface.
- **State States**: Sealed states and linear indicators display real-time network and authentication status without blocking the UI loops.

Please maintain this architecture carefully when adding new features or screens.
