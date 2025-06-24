#!/bin/bash

# EMFAD® APK Build - Original Windows UI mit AR-Button
# Samsung S21 Ultra optimiert

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

echo -e "${BLUE}🔧 EMFAD® APK Build - Original Windows UI${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "${CYAN}Building APK with Original EMFAD Windows UI + AR-Button${NC}"
echo ""

# Check if we're in the right directory
if [ ! -f "build.gradle" ]; then
    echo -e "${RED}❌ build.gradle not found. Please run from project root.${NC}"
    exit 1
fi

# Check Samsung S21 Ultra connection
check_device() {
    echo -e "${BLUE}📱 Checking Samsung S21 Ultra...${NC}"
    
    if command -v adb &> /dev/null; then
        if adb devices | grep -q "device$"; then
            local device_model=$(adb shell getprop ro.product.model 2>/dev/null || echo "Unknown")
            local device_id=$(adb devices | grep "device$" | awk '{print $1}')
            
            if [[ "$device_model" == *"SM-G998"* ]]; then
                echo -e "${GREEN}✅ Samsung S21 Ultra connected${NC}"
                echo -e "   Model: ${GREEN}$device_model${NC}"
                echo -e "   Device ID: ${GREEN}$device_id${NC}"
                return 0
            else
                echo -e "${YELLOW}⚠️  Connected device: $device_model${NC}"
                return 0
            fi
        else
            echo -e "${YELLOW}⚠️  No device connected${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠️  ADB not found${NC}"
        return 1
    fi
}

# Create Gradle Wrapper if needed
setup_gradle() {
    echo -e "${BLUE}🔧 Setting up Gradle...${NC}"
    
    if [ ! -f "gradlew" ]; then
        echo -e "${YELLOW}⚠️  Gradle wrapper not found, creating...${NC}"
        
        # Create gradle wrapper properties
        mkdir -p gradle/wrapper
        
        cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

        # Try to download gradle wrapper jar if curl is available
        if command -v curl &> /dev/null; then
            echo "Downloading Gradle wrapper..."
            curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.0.0/gradle/wrapper/gradle-wrapper.jar 2>/dev/null || echo "Download failed, continuing..."
        fi
        
        # Create gradlew script
        cat > gradlew << 'EOF'
#!/bin/sh
exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
EOF
        chmod +x gradlew
    fi
    
    if [ -f "gradlew" ]; then
        echo -e "${GREEN}✅ Gradle wrapper ready${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  Gradle wrapper setup incomplete${NC}"
        return 1
    fi
}

# Build APK
build_apk() {
    echo -e "${BLUE}🔨 Building EMFAD APK...${NC}"
    
    # Clean previous builds
    echo "Cleaning previous builds..."
    rm -rf build/
    
    # Try different build methods
    local build_success=false
    
    # Method 1: Gradle wrapper
    if [ -f "gradlew" ] && [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
        echo -e "${CYAN}📦 Trying Gradle wrapper...${NC}"
        if ./gradlew clean assembleDebug 2>/dev/null; then
            build_success=true
            echo -e "${GREEN}✅ Gradle wrapper build successful${NC}"
        else
            echo -e "${YELLOW}⚠️  Gradle wrapper build failed${NC}"
        fi
    fi
    
    # Method 2: System Gradle
    if [ "$build_success" = false ] && command -v gradle &> /dev/null; then
        echo -e "${CYAN}📦 Trying system Gradle...${NC}"
        if gradle clean assembleDebug 2>/dev/null; then
            build_success=true
            echo -e "${GREEN}✅ System Gradle build successful${NC}"
        else
            echo -e "${YELLOW}⚠️  System Gradle build failed${NC}"
        fi
    fi
    
    # Method 3: Android Studio command line
    if [ "$build_success" = false ]; then
        local android_studio_gradle="/Applications/Android Studio.app/Contents/gradle/gradle-*/bin/gradle"
        if ls $android_studio_gradle 1> /dev/null 2>&1; then
            echo -e "${CYAN}📦 Trying Android Studio Gradle...${NC}"
            local gradle_path=$(ls $android_studio_gradle | head -1)
            if "$gradle_path" clean assembleDebug 2>/dev/null; then
                build_success=true
                echo -e "${GREEN}✅ Android Studio Gradle build successful${NC}"
            else
                echo -e "${YELLOW}⚠️  Android Studio Gradle build failed${NC}"
            fi
        fi
    fi
    
    if [ "$build_success" = false ]; then
        echo -e "${RED}❌ All build methods failed${NC}"
        echo -e "${CYAN}💡 Try opening the project in Android Studio and building manually${NC}"
        return 1
    fi
    
    # Check if APK was created
    local apk_path=""
    if [ -f "build/outputs/apk/debug/app-debug.apk" ]; then
        apk_path="build/outputs/apk/debug/app-debug.apk"
    elif [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
        apk_path="build/outputs/apk/debug/com.emfad.app-debug.apk"
    fi
    
    if [ -n "$apk_path" ]; then
        local apk_size=$(du -h "$apk_path" | cut -f1)
        echo -e "${GREEN}✅ APK created successfully${NC}"
        echo -e "   Path: ${GREEN}$apk_path${NC}"
        echo -e "   Size: ${GREEN}$apk_size${NC}"
        return 0
    else
        echo -e "${RED}❌ APK not found after build${NC}"
        return 1
    fi
}

# Install APK on Samsung S21 Ultra
install_apk() {
    echo -e "${BLUE}📲 Installing APK on Samsung S21 Ultra...${NC}"
    
    # Find APK
    local apk_path=""
    if [ -f "build/outputs/apk/debug/app-debug.apk" ]; then
        apk_path="build/outputs/apk/debug/app-debug.apk"
    elif [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
        apk_path="build/outputs/apk/debug/com.emfad.app-debug.apk"
    else
        echo -e "${RED}❌ APK not found${NC}"
        return 1
    fi
    
    # Install APK
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
                echo -e "${GREEN}✅ EMFAD App started successfully (PID: $app_pid)${NC}"
                
                # Show logs
                echo -e "${BLUE}📋 Recent app logs:${NC}"
                adb logcat -d | grep -i "emfad\|timber\|MainActivity" | tail -5
                
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
    echo -e "${BLUE}│${NC}  tools   ${BLUE}│${NC} spectrum ${BLUE}│${NC}   path   ${BLUE}│${NC}    ${MAGENTA}AR${NC}    ${BLUE}│${NC}"
    echo -e "${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}"
    echo -e "${BLUE}├──────────┼──────────┼──────────┼──────────┤${NC}"
    echo -e "${BLUE}│${NC}  setup   ${BLUE}│${NC}scan 2D/3D${BLUE}│${NC}   map    ${BLUE}│${NC} EMTOMO   ${BLUE}│${NC}"
    echo -e "${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}          ${BLUE}│${NC}"
    echo -e "${BLUE}└──────────┴──────────┴──────────┴──────────┘${NC}"
    echo ""
    echo -e "${BLUE}┌─────────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│${NC} ${RED}[close application]${NC}  antenna A parallel filter 1 ${BLUE}│${NC}"
    echo -e "${BLUE}└─────────────────────────────────────────┘${NC}"
    echo ""
    echo -e "${MAGENTA}✨ AR-Button hinzugefügt für Samsung S21 Ultra!${NC}"
}

# Main process
main() {
    echo -e "${BLUE}Starting EMFAD APK Build Process...${NC}"
    echo ""
    
    # Check device
    local device_connected=false
    if check_device; then
        device_connected=true
    fi
    
    echo ""
    
    # Setup Gradle
    if ! setup_gradle; then
        echo -e "${YELLOW}⚠️  Gradle setup incomplete, trying anyway...${NC}"
    fi
    
    echo ""
    
    # Build APK
    if ! build_apk; then
        echo -e "${RED}❌ APK build failed${NC}"
        exit 1
    fi
    
    echo ""
    
    # Install if device connected
    if [ "$device_connected" = true ]; then
        if ! install_apk; then
            echo -e "${RED}❌ Installation failed${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}⚠️  No device connected, skipping installation${NC}"
        echo -e "${CYAN}📱 Connect Samsung S21 Ultra and run:${NC}"
        echo "   adb install -r build/outputs/apk/debug/app-debug.apk"
    fi
    
    echo ""
    show_expected_ui
    
    echo ""
    echo -e "${GREEN}🎉 EMFAD APK Build Complete! 🎉${NC}"
    echo -e "${BLUE}================================${NC}"
    echo -e "📱 APK: ${GREEN}Ready for Samsung S21 Ultra${NC}"
    echo -e "🎨 UI: ${GREEN}Original EMFAD Windows + AR-Button${NC}"
    echo -e "🔧 Features: ${GREEN}13 Function Buttons${NC}"
    echo -e "📊 Performance: ${GREEN}Samsung S21 Ultra optimized${NC}"
    echo ""
    echo -e "${CYAN}Das originale EMFAD Windows UI mit AR-Button ist bereit!${NC}"
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Build interrupted${NC}"; exit 1' INT

# Run main process
main "$@"
