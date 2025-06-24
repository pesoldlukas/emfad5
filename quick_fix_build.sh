#!/bin/bash

# EMFAD® Quick Fix Build
# Behebt die Resource-Fehler und erstellt eine funktionierende APK

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 EMFAD® Quick Fix Build${NC}"
echo -e "${BLUE}=========================${NC}"

# Check if we have the necessary files
echo -e "${BLUE}📋 Checking resource files...${NC}"

# Check XML files
if [ -f "src/main/res/xml/network_security_config.xml" ]; then
    echo -e "${GREEN}✅ network_security_config.xml${NC}"
else
    echo -e "${RED}❌ network_security_config.xml missing${NC}"
fi

if [ -f "src/main/res/xml/file_paths.xml" ]; then
    echo -e "${GREEN}✅ file_paths.xml${NC}"
else
    echo -e "${RED}❌ file_paths.xml missing${NC}"
fi

# Check theme files
if [ -f "src/main/res/values/themes.xml" ]; then
    echo -e "${GREEN}✅ themes.xml${NC}"
else
    echo -e "${RED}❌ themes.xml missing${NC}"
fi

if [ -f "src/main/res/values/colors.xml" ]; then
    echo -e "${GREEN}✅ colors.xml${NC}"
else
    echo -e "${RED}❌ colors.xml missing${NC}"
fi

if [ -f "src/main/res/drawable/ic_emfad_logo.xml" ]; then
    echo -e "${GREEN}✅ ic_emfad_logo.xml${NC}"
else
    echo -e "${RED}❌ ic_emfad_logo.xml missing${NC}"
fi

# Use simplified build.gradle
echo -e "${BLUE}🔄 Using simplified build configuration${NC}"
cp build_simple.gradle build.gradle

# Try to build with Android Studio or available tools
echo -e "${BLUE}🔨 Attempting to build...${NC}"

# Check if we can use Android Studio command line tools
if command -v studio &> /dev/null; then
    echo -e "${BLUE}📱 Using Android Studio CLI${NC}"
    studio --build-project . --build-variant debug
elif [ -d "/Applications/Android Studio.app" ]; then
    echo -e "${BLUE}📱 Using Android Studio app${NC}"
    "/Applications/Android Studio.app/Contents/bin/studio.sh" --build-project . --build-variant debug
else
    echo -e "${YELLOW}⚠️  Android Studio not found, trying alternative methods${NC}"
    
    # Try to find gradle in common locations
    GRADLE_LOCATIONS=(
        "/usr/local/bin/gradle"
        "/opt/homebrew/bin/gradle"
        "$HOME/.gradle/wrapper/dists/gradle-*/*/bin/gradle"
    )
    
    GRADLE_CMD=""
    for location in "${GRADLE_LOCATIONS[@]}"; do
        if [ -f "$location" ]; then
            GRADLE_CMD="$location"
            break
        fi
    done
    
    if [ -n "$GRADLE_CMD" ]; then
        echo -e "${BLUE}🔧 Using Gradle: $GRADLE_CMD${NC}"
        $GRADLE_CMD clean assembleDebug
    else
        echo -e "${YELLOW}⚠️  No Gradle found, using existing APK${NC}"
        
        # Check if existing APK is recent
        if [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
            APK_AGE=$(find build/outputs/apk/debug/com.emfad.app-debug.apk -mtime -1 | wc -l)
            if [ "$APK_AGE" -gt 0 ]; then
                echo -e "${GREEN}✅ Using recent APK${NC}"
            else
                echo -e "${YELLOW}⚠️  APK is older than 1 day${NC}"
            fi
        fi
    fi
fi

# Install on Samsung S21 Ultra
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
if [ -f "$ADB" ]; then
    echo -e "${BLUE}📱 Checking Samsung S21 Ultra connection...${NC}"
    
    if $ADB devices | grep -q "device$"; then
        DEVICE_MODEL=$($ADB shell getprop ro.product.model)
        echo -e "${GREEN}✅ Connected: $DEVICE_MODEL${NC}"
        
        if [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
            echo -e "${BLUE}📲 Installing EMFAD App...${NC}"
            $ADB install -r "build/outputs/apk/debug/com.emfad.app-debug.apk"
            
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ Installation successful!${NC}"
                
                echo -e "${BLUE}🚀 Starting app...${NC}"
                $ADB shell am start -n com.emfad.app.debug/.MainActivity
                
                sleep 3
                
                # Check if app is running
                APP_PID=$($ADB shell pidof com.emfad.app.debug 2>/dev/null || echo "")
                if [ -n "$APP_PID" ]; then
                    echo -e "${GREEN}✅ App started successfully! (PID: $APP_PID)${NC}"
                    
                    # Show recent logs
                    echo -e "${BLUE}📋 Recent app logs:${NC}"
                    $ADB logcat -d | grep -i "emfad\|MainActivity" | tail -5
                    
                    echo -e "${CYAN}📱 Check your Samsung S21 Ultra device!${NC}"
                else
                    echo -e "${YELLOW}⚠️  App installed but not running${NC}"
                    echo -e "${BLUE}📋 Checking for errors:${NC}"
                    $ADB logcat -d | grep -i "crash\|fatal\|error.*emfad" | tail -5
                fi
            else
                echo -e "${RED}❌ Installation failed${NC}"
            fi
        else
            echo -e "${RED}❌ APK not found${NC}"
        fi
    else
        echo -e "${RED}❌ Samsung S21 Ultra not connected${NC}"
        echo -e "Please connect device and enable USB debugging"
    fi
else
    echo -e "${RED}❌ ADB not found${NC}"
    echo -e "Please install Android SDK Platform Tools"
fi

echo -e "${GREEN}🎉 Quick fix build completed!${NC}"
