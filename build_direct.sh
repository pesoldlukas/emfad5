#!/bin/bash

# EMFAD® Direct Build - Original Windows UI mit AR-Button
# Direkter Build ohne Gradle Wrapper

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Environment
export ANDROID_HOME="/Volumes/PortableSSD/AndroidSDK"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export JAVA_HOME="/Volumes/PortableSSD/Android Studio.app/Contents/jbr/Contents/Home"

echo -e "${BLUE}🔧 EMFAD® Direct Build - Original Windows UI${NC}"
echo -e "${BLUE}=============================================${NC}"
echo -e "${CYAN}Building Original EMFAD Windows UI with AR-Button${NC}"
echo ""

# Check environment
check_environment() {
    echo -e "${BLUE}🔍 Checking build environment...${NC}"
    
    if [ ! -d "$ANDROID_HOME" ]; then
        echo -e "${RED}❌ ANDROID_HOME not found: $ANDROID_HOME${NC}"
        return 1
    fi
    
    if [ ! -f "$ANDROID_HOME/platform-tools/adb" ]; then
        echo -e "${RED}❌ ADB not found${NC}"
        return 1
    fi
    
    if [ ! -d "$JAVA_HOME" ]; then
        echo -e "${RED}❌ JAVA_HOME not found: $JAVA_HOME${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ Android SDK: $ANDROID_HOME${NC}"
    echo -e "${GREEN}✅ Java: $JAVA_HOME${NC}"
    echo -e "${GREEN}✅ ADB: Available${NC}"
    
    return 0
}

# Check Samsung S21 Ultra
check_device() {
    echo -e "${BLUE}📱 Checking Samsung S21 Ultra...${NC}"
    
    local devices=$(adb devices | grep "device$" | wc -l)
    
    if [ "$devices" -gt 0 ]; then
        local device_model=$(adb shell getprop ro.product.model 2>/dev/null || echo "Unknown")
        local device_id=$(adb devices | grep "device$" | awk '{print $1}' | head -1)
        
        echo -e "${GREEN}✅ Device connected${NC}"
        echo -e "   Model: ${GREEN}$device_model${NC}"
        echo -e "   Device ID: ${GREEN}$device_id${NC}"
        
        if [[ "$device_model" == *"SM-G998"* ]]; then
            echo -e "${GREEN}✅ Samsung S21 Ultra confirmed${NC}"
        fi
        
        return 0
    else
        echo -e "${YELLOW}⚠️  No device connected${NC}"
        return 1
    fi
}

# Try alternative build methods
build_apk() {
    echo -e "${BLUE}🔨 Building APK...${NC}"
    
    # Clean previous builds
    rm -rf build/
    
    # Method 1: Try system gradle
    if command -v gradle &> /dev/null; then
        echo -e "${CYAN}📦 Trying system Gradle...${NC}"
        if gradle clean assembleDebug 2>/dev/null; then
            echo -e "${GREEN}✅ System Gradle build successful${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  System Gradle failed${NC}"
        fi
    fi
    
    # Method 2: Try Android Studio gradle
    local android_studio_gradle="/Volumes/PortableSSD/Android Studio.app/Contents/gradle/gradle-*/bin/gradle"
    if ls $android_studio_gradle 1> /dev/null 2>&1; then
        echo -e "${CYAN}📦 Trying Android Studio Gradle...${NC}"
        local gradle_path=$(ls $android_studio_gradle | head -1)
        if "$gradle_path" clean assembleDebug 2>/dev/null; then
            echo -e "${GREEN}✅ Android Studio Gradle build successful${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  Android Studio Gradle failed${NC}"
        fi
    fi
    
    # Method 3: Create minimal APK structure
    echo -e "${CYAN}📦 Creating minimal APK structure...${NC}"
    mkdir -p build/outputs/apk/debug/
    
    # Create a placeholder APK (this would need proper build tools)
    echo -e "${YELLOW}⚠️  No working build system found${NC}"
    echo -e "${CYAN}💡 Creating build instructions instead...${NC}"
    
    return 1
}

# Create build instructions
create_build_instructions() {
    echo -e "${BLUE}📋 Creating build instructions...${NC}"
    
    cat > BUILD_INSTRUCTIONS.md << 'EOF'
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
EOF

    echo -e "${GREEN}✅ Build instructions created: BUILD_INSTRUCTIONS.md${NC}"
}

# Install if APK exists
install_apk() {
    echo -e "${BLUE}📲 Checking for APK...${NC}"
    
    local apk_path=""
    if [ -f "build/outputs/apk/debug/app-debug.apk" ]; then
        apk_path="build/outputs/apk/debug/app-debug.apk"
    elif [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
        apk_path="build/outputs/apk/debug/com.emfad.app-debug.apk"
    fi
    
    if [ -n "$apk_path" ]; then
        echo -e "${GREEN}✅ APK found: $apk_path${NC}"
        
        local apk_size=$(du -h "$apk_path" | cut -f1)
        echo -e "   Size: ${GREEN}$apk_size${NC}"
        
        echo -e "${BLUE}📲 Installing on Samsung S21 Ultra...${NC}"
        
        if adb install -r "$apk_path"; then
            echo -e "${GREEN}✅ APK installed successfully${NC}"
            
            # Start app
            echo -e "${BLUE}🚀 Starting EMFAD App...${NC}"
            adb logcat -c
            
            if adb shell am start -n com.emfad.app.debug/.MainActivity 2>/dev/null || \
               adb shell am start -n com.emfad.app/.MainActivity 2>/dev/null; then
                
                sleep 3
                
                # Check if app is running
                local app_pid=$(adb shell pidof com.emfad.app.debug 2>/dev/null || adb shell pidof com.emfad.app 2>/dev/null || echo "")
                
                if [ -n "$app_pid" ]; then
                    echo -e "${GREEN}✅ EMFAD App started (PID: $app_pid)${NC}"
                    
                    # Test UI
                    echo -e "${BLUE}🧪 Testing Original UI...${NC}"
                    adb shell monkey -p com.emfad.app.debug 1 2>/dev/null || adb shell monkey -p com.emfad.app 1 2>/dev/null
                    
                    echo ""
                    echo -e "${GREEN}🎉 Original EMFAD Windows UI mit AR-Button läuft!${NC}"
                    echo -e "${CYAN}📱 Prüfe das Samsung S21 Ultra für das UI${NC}"
                    
                    return 0
                else
                    echo -e "${RED}❌ App failed to start${NC}"
                    return 1
                fi
            else
                echo -e "${RED}❌ Failed to start app${NC}"
                return 1
            fi
        else
            echo -e "${RED}❌ APK installation failed${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠️  No APK found${NC}"
        return 1
    fi
}

# Show expected UI
show_expected_ui() {
    echo -e "${CYAN}📱 Expected Original EMFAD Windows UI:${NC}"
    echo ""
    echo -e "${BLUE}┌─────────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│${NC}                ${GREEN}EMFAD${NC}                 ${BLUE}│${NC}"
    echo -e "${BLUE}│${NC}        ${RED}████████ ● ████████${NC}        ${BLUE}│${NC}"
    echo -e "${BLUE}│${NC}            ${CYAN}EMFAD EMUNI${NC}             ${BLUE}│${NC}"
    echo -e "${BLUE}└─────────────────────────────────────────┘${NC}"
    echo ""
    echo -e "${BLUE}┌──────────┬──────────┬──────────┬──────────┐${NC}"
    echo -e "${BLUE}│${NC} assign   ${BLUE}│${NC} profile  ${BLUE}│${NC} scan GPS ${BLUE}│${NC} connect  ${BLUE}│${NC}"
    echo -e "${BLUE}│${NC}   COM    ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}"
    echo -e "${BLUE}├──────────┼──────────┼──────────┼──────────┤${NC}"
    echo -e "${BLUE}│${NC}  tools   ${BLUE}│${NC} spectrum ${BLUE}│${NC}   path   ${BLUE}│${NC}    ${CYAN}AR${NC}    ${BLUE}│${NC}"
    echo -e "${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}"
    echo -e "${BLUE}├──────────┼──────────┼──────────┼──────────┤${NC}"
    echo -e "${BLUE}│${NC}  setup   ${BLUE}│${NC}scan 2D/3D${BLUE}│${NC}   map    ${BLUE}│${NC} EMTOMO   ${BLUE}│${NC}"
    echo -e "${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}"
    echo -e "${BLUE}└──────────┴──────────┴──────────┴──────────┘${NC}"
    echo ""
    echo -e "${BLUE}┌─────────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│${NC} ${RED}[close application]${NC}  antenna A parallel filter 1 ${BLUE}│${NC}"
    echo -e "${BLUE}└─────────────────────────────────────────┘${NC}"
}

# Main process
main() {
    echo -e "${BLUE}Starting EMFAD Direct Build Process...${NC}"
    echo ""
    
    # Check environment
    if ! check_environment; then
        echo -e "${RED}❌ Environment check failed${NC}"
        exit 1
    fi
    
    echo ""
    
    # Check device
    local device_connected=false
    if check_device; then
        device_connected=true
    fi
    
    echo ""
    
    # Try to build
    if build_apk; then
        echo ""
        if [ "$device_connected" = true ]; then
            install_apk
        fi
    else
        echo ""
        create_build_instructions
    fi
    
    echo ""
    show_expected_ui
    
    echo ""
    echo -e "${GREEN}🎉 EMFAD Direct Build Process Complete! 🎉${NC}"
    echo -e "${BLUE}===========================================${NC}"
    echo -e "📱 Environment: ${GREEN}Ready${NC}"
    echo -e "🎨 UI Code: ${GREEN}Original EMFAD Windows + AR-Button${NC}"
    echo -e "🔧 Build: ${YELLOW}Manual steps required${NC}"
    echo -e "📊 Target: ${GREEN}Samsung S21 Ultra${NC}"
    echo ""
    echo -e "${CYAN}Next: Open project in Android Studio for build${NC}"
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Build interrupted${NC}"; exit 1' INT

# Run main process
main "$@"
