# EMFAD® Build Instructions

## Environment Setup ✅
- ANDROID_HOME: /Volumes/PortableSSD/AndroidSDK
- JAVA_HOME: /Volumes/PortableSSD/Android Studio.app/Contents/jbr/Contents/Home
- ADB: Available

## Manual Build Steps

### Option 1: Android Studio (Recommended)
1. Open Android Studio
2. File → Open → /Volumes/PortableSSD/emfad3/com.emfad.app
3. Wait for Gradle sync
4. Connect Samsung S21 Ultra
5. Run → Run 'app'

### Option 2: Command Line
```bash
export ANDROID_HOME="/Volumes/PortableSSD/AndroidSDK"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export JAVA_HOME="/Volumes/PortableSSD/Android Studio.app/Contents/jbr/Contents/Home"

cd /Volumes/PortableSSD/emfad3/com.emfad.app
./gradlew clean assembleDebug
```

### Option 3: Direct Gradle
```bash
gradle clean assembleDebug
adb install -r build/outputs/apk/debug/app-debug.apk
```

## Expected Result
Original EMFAD Windows UI with AR-Button on Samsung S21 Ultra
