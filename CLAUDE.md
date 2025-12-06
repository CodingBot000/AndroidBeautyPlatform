# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android WebView application for Mimotok (https://www.mimotok.com) built with Jetpack Compose and Kotlin. The app provides a native wrapper around the Mimotok web app with camera/gallery permissions for file uploads and a bottom navigation interface.

## Key Architecture

### WebView Integration
- `ComposeWebView`: Main WebView composable with file upload support, progress tracking, and JavaScript interface
- `WebViewContainer`: Container wrapper (currently unused but provides layout structure)
- `WebAppInterface`: JavaScript bridge for communication between web and native layers

### Permission Management
- `CameraGalleryPermissionRequester`: Handles camera and storage permissions with different behavior based on Android version (API 33+ uses READ_MEDIA_IMAGES, older uses READ_EXTERNAL_STORAGE)
- Supports automatic permission requests and permanent denial detection
- `PermissionController`: Global singleton for permission re-requests and app settings navigation

### Navigation Structure
- `MainActivity`: Main entry point with edge-to-edge display configuration
- `AppScreen`: Bottom navigation with 6 tabs (Home, Treatment-Info, Community, List, Diagnosis, MyPage)
- Each tab loads a different URL from the Mimotok domain
- Back button handling integrated with WebView history

### Component Organization
- Main source: `app/src/main/java/com/example/myapplication/`
- Theme configuration in `ui/theme/` directory
- Uses Material 3 design system with Jetpack Compose

## Build Commands

### Development
```bash
# Build debug APK
./gradlew assembleDebug

# Install debug APK to connected device
./gradlew installDebug

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Release
```bash
# Build release APK
./gradlew assembleRelease
```

## Project Configuration

- **Namespace**: `com.beauty.platform`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java Version**: 11
- **Kotlin**: 2.0.21
- **AGP**: 8.12.1

## Dependencies

Built with modern Android development stack:
- Jetpack Compose BOM 2024.09.00
- Material 3
- Activity Compose
- Core KTX
- Lifecycle Runtime KTX

Testing:
- JUnit 4.13.2 for unit tests
- AndroidX JUnit and Espresso for instrumented tests

## Important Implementation Details

### File Upload Handling
The WebView supports file uploads through `onShowFileChooser` implementation with:
- Single and multiple file selection
- MIME type filtering based on web page requirements
- ActivityResult API for modern permission handling

### Permission Flow
1. App checks camera and storage permissions on startup
2. Shows permission request dialog if needed
3. Handles permanent denials by directing users to app settings
4. Different permission sets for Android 13+ vs older versions

### WebView Configuration
- JavaScript enabled with DOM storage
- Custom User Agent string: includes "MyAppWebView/1.0 (Android)"
- File access enabled for uploads
- Edge-to-edge display with proper inset handling

### Navigation Behavior
- Bottom navigation controls WebView URL loading
- WebView back button integration
- Progress indicators during page loads
- Error handling for network issues