# AuraLens - AI Vision Assistant

**AuraLens** is an Android application designed to assist blind and low-vision users by providing real-time audio descriptions of their surroundings, reading text, and finding objects using Google's **Gemini 3** models.

## Features
- **Explore Scene**: Point the camera to get a concise or detailed description of the environment.
- **Read Text**: High-resolution text recognition for reading documents, labels, and signs.
- **Find Object**: Voice-guided object locator. Ask for an object (e.g., "Keys") and get directional cues.
- **Ask About Photo**: Take a photo and ask any question about it using natural language.
- **Accessibility**: Full TalkBack support, high-contrast UI, and automatic Text-to-Speech (TTS).

## How to Build
1.  **Prerequisites**: Ensure you have **JDK 17** installed (required by Android Gradle Plugin 8.2+).
2.  Open the project in Android Studio (Iguana or later).
3.  Open `MainActivity.kt` and replace `YOUR_API_KEY_HERE` with your valid Gemini 3 API Key.
    -   Get a key from [Google AI Studio](https://aistudio.google.com/).
4.  Sync Gradle and Run on a device or emulator with Camera support.

## Command Line Build
If you prefer the terminal or want to build a release APK:

1.  **Generate Wrapper** (Critical step to ensure compatibility):
    ```bash
    gradle wrapper --gradle-version 8.2
    ```
2.  **Debug APK**:
    ```bash
    ./gradlew assembleDebug
    ```
3.  **Install on Device**:
    ```bash
    ./gradlew installDebug
    ```

## Architecture
-   **MVVM**: Separation of UI (Compose), State (ViewModel), and Data (Repository).
-   **Gemini Integration**: Uses `google-ai-client` SDK for Android.
-   **CameraX**: For efficient image capture and preview.
-   **Accessibility**: Native Android TTS and SpeechRecognizer integration.

## Technologies
-   Kotlin, Jetpack Compose, Material 3
-   Gemini 3 Pro / Flash Models
-   CameraX, TextToSpeech, SpeechRecognizer

## Judging Criteria Alignment
-   **Innovation**: Uses the latest Gemini 3 multimodal capabilities (Vision + Language) to solve a daily accessibility challenge.
-   **Impact**: Directly improves quality of life for 250M+ visually impaired people.
-   **Execution**: Production-quality architecture, error handling, and offline-first UI design.
