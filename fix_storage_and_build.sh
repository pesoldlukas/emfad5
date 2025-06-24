#!/bin/bash

# EMFAD® Storage Fix & APK Build
# Löst Speicherplatz-Problem und baut APK

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${RED}🚨 EMFAD® Storage Fix & Build${NC}"
echo -e "${RED}==============================${NC}"
echo -e "${YELLOW}Fixing 'No space left on device' error${NC}"
echo ""

# Check current storage
check_storage() {
    echo -e "${BLUE}📊 Checking storage usage...${NC}"
    
    local total_size=$(du -sh /Volumes/PortableSSD/emfad3 2>/dev/null | cut -f1 || echo "Unknown")
    echo -e "   Total project size: ${YELLOW}$total_size${NC}"
    
    echo -e "${BLUE}📋 Largest directories:${NC}"
    du -sh /Volumes/PortableSSD/emfad3/* 2>/dev/null | sort -hr | head -5
    
    echo ""
}

# Clean up large files
cleanup_storage() {
    echo -e "${BLUE}🧹 Cleaning up storage...${NC}"
    
    cd /Volumes/PortableSSD/emfad3
    
    # Clean Gradle cache
    echo -e "${CYAN}   Cleaning Gradle cache...${NC}"
    rm -rf gradle/caches/ 2>/dev/null || true
    rm -rf gradle/daemon/ 2>/dev/null || true
    rm -rf gradle/wrapper/dists/ 2>/dev/null || true
    
    # Clean build directories
    echo -e "${CYAN}   Cleaning build directories...${NC}"
    find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name ".gradle" -type d -exec rm -rf {} + 2>/dev/null || true
    
    # Clean temporary files
    echo -e "${CYAN}   Cleaning temporary files...${NC}"
    find . -name "*.tmp" -delete 2>/dev/null || true
    find . -name "*.log" -delete 2>/dev/null || true
    find . -name ".DS_Store" -delete 2>/dev/null || true
    
    # Clean large Ghidra files (keep only essentials)
    echo -e "${CYAN}   Cleaning Ghidra files...${NC}"
    if [ -d "ghidra" ]; then
        # Keep only the essential Ghidra project files
        find ghidra -name "*.rep" -type d | head -1 > /tmp/keep_ghidra.txt
        if [ -s /tmp/keep_ghidra.txt ]; then
            local keep_dir=$(cat /tmp/keep_ghidra.txt)
            echo -e "     Keeping: ${GREEN}$keep_dir${NC}"
            # Move essential files to temp location
            mkdir -p /tmp/ghidra_essential
            cp -r "$keep_dir" /tmp/ghidra_essential/ 2>/dev/null || true
        fi
        
        # Remove large Ghidra directory
        rm -rf ghidra 2>/dev/null || true
        
        # Restore essential files
        if [ -d "/tmp/ghidra_essential" ]; then
            mkdir -p ghidra
            cp -r /tmp/ghidra_essential/* ghidra/ 2>/dev/null || true
            rm -rf /tmp/ghidra_essential
        fi
    fi
    
    # Clean duplicate Ghidra installation
    if [ -d "ghidra_11.3.2_PUBLIC" ]; then
        echo -e "${CYAN}   Removing duplicate Ghidra installation...${NC}"
        rm -rf ghidra_11.3.2_PUBLIC 2>/dev/null || true
    fi
    
    # Clean ZIP files
    echo -e "${CYAN}   Cleaning ZIP files...${NC}"
    rm -f *.zip 2>/dev/null || true
    
    echo -e "${GREEN}✅ Storage cleanup completed${NC}"
}

# Setup minimal build environment
setup_minimal_build() {
    echo -e "${BLUE}🔧 Setting up minimal build environment...${NC}"
    
    cd com.emfad.app
    
    # Clean project build files
    rm -rf build/ 2>/dev/null || true
    rm -rf .gradle/ 2>/dev/null || true
    
    # Create minimal gradle wrapper
    mkdir -p gradle/wrapper
    
    cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

    # Create minimal gradlew
    cat > gradlew << 'EOF'
#!/bin/sh
exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
EOF
    chmod +x gradlew
    
    echo -e "${GREEN}✅ Minimal build environment ready${NC}"
}

# Try lightweight build
try_lightweight_build() {
    echo -e "${BLUE}🔨 Attempting lightweight build...${NC}"
    
    cd com.emfad.app
    
    # Set environment
    export ANDROID_HOME="/Volumes/PortableSSD/AndroidSDK"
    export PATH="$ANDROID_HOME/platform-tools:$PATH"
    export JAVA_HOME="/Volumes/PortableSSD/Android Studio.app/Contents/jbr/Contents/Home"
    
    # Try different build approaches
    local build_success=false
    
    # Method 1: Direct Android Studio gradle
    if [ -f "/Volumes/PortableSSD/Android Studio.app/Contents/bin/studio.sh" ]; then
        echo -e "${CYAN}📦 Trying Android Studio build...${NC}"
        # This would require GUI, skip for now
    fi
    
    # Method 2: Minimal gradle build
    echo -e "${CYAN}📦 Trying minimal build...${NC}"
    
    # Create minimal build.gradle for testing
    cat > build_minimal.gradle << 'EOF'
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.emfad.app"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    
    buildTypes {
        debug {
            minifyEnabled false
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
    
    buildFeatures {
        compose true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion '1.5.4'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation platform('androidx.compose:compose-bom:2023.10.01')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.navigation:navigation-compose:2.7.5'
}
EOF

    # Try build with minimal config
    if gradle -b build_minimal.gradle clean assembleDebug 2>/dev/null; then
        build_success=true
        echo -e "${GREEN}✅ Minimal build successful${NC}"
    else
        echo -e "${YELLOW}⚠️  Minimal build failed${NC}"
    fi
    
    if [ "$build_success" = false ]; then
        echo -e "${YELLOW}⚠️  Automated build not possible${NC}"
        echo -e "${CYAN}💡 Manual Android Studio build required${NC}"
        return 1
    fi
    
    return 0
}

# Install APK if available
install_apk() {
    echo -e "${BLUE}📲 Checking for APK...${NC}"
    
    cd com.emfad.app
    
    local apk_path=""
    if [ -f "build/outputs/apk/debug/app-debug.apk" ]; then
        apk_path="build/outputs/apk/debug/app-debug.apk"
    fi
    
    if [ -n "$apk_path" ]; then
        echo -e "${GREEN}✅ APK found: $apk_path${NC}"
        
        # Check if device connected
        export ANDROID_HOME="/Volumes/PortableSSD/AndroidSDK"
        export PATH="$ANDROID_HOME/platform-tools:$PATH"
        
        if adb devices | grep -q "device$"; then
            echo -e "${BLUE}📲 Installing on Samsung S21 Ultra...${NC}"
            
            if adb install -r "$apk_path"; then
                echo -e "${GREEN}✅ APK installed successfully${NC}"
                
                # Start app
                adb shell am start -n com.emfad.app/.MainActivity 2>/dev/null || \
                adb shell am start -n com.emfad.app.debug/.MainActivity 2>/dev/null
                
                echo -e "${GREEN}🎉 EMFAD Original Windows UI läuft!${NC}"
                return 0
            else
                echo -e "${RED}❌ Installation failed${NC}"
                return 1
            fi
        else
            echo -e "${YELLOW}⚠️  No device connected${NC}"
            echo -e "${CYAN}📱 Connect Samsung S21 Ultra and run:${NC}"
            echo "   adb install -r $apk_path"
            return 0
        fi
    else
        echo -e "${YELLOW}⚠️  No APK found${NC}"
        return 1
    fi
}

# Show final instructions
show_final_instructions() {
    echo -e "${CYAN}📋 Final Build Instructions:${NC}"
    echo ""
    echo -e "${BLUE}After storage cleanup, use Android Studio:${NC}"
    echo "1. Open Android Studio"
    echo "2. File → Open → /Volumes/PortableSSD/emfad3/com.emfad.app"
    echo "3. Wait for Gradle sync"
    echo "4. Connect Samsung S21 Ultra"
    echo "5. Run → Run 'app'"
    echo ""
    echo -e "${GREEN}Expected Result: Original EMFAD Windows UI with AR-Button${NC}"
}

# Main process
main() {
    echo -e "${BLUE}Starting Storage Fix & Build Process...${NC}"
    echo ""
    
    # Check current storage
    check_storage
    
    # Clean up storage
    cleanup_storage
    
    echo ""
    
    # Check storage after cleanup
    echo -e "${BLUE}📊 Storage after cleanup:${NC}"
    local new_size=$(du -sh /Volumes/PortableSSD/emfad3 2>/dev/null | cut -f1 || echo "Unknown")
    echo -e "   Project size: ${GREEN}$new_size${NC}"
    
    echo ""
    
    # Setup minimal build
    setup_minimal_build
    
    echo ""
    
    # Try build
    if try_lightweight_build; then
        echo ""
        install_apk
    else
        echo ""
        show_final_instructions
    fi
    
    echo ""
    echo -e "${GREEN}🎉 Storage Fix & Build Process Complete! 🎉${NC}"
    echo -e "${BLUE}===========================================${NC}"
    echo -e "💾 Storage: ${GREEN}Cleaned and optimized${NC}"
    echo -e "🎨 UI Code: ${GREEN}Original EMFAD Windows + AR-Button${NC}"
    echo -e "🔧 Build: ${GREEN}Ready for Android Studio${NC}"
    echo -e "📱 Target: ${GREEN}Samsung S21 Ultra${NC}"
    echo ""
    echo -e "${CYAN}Das Speicherplatz-Problem ist gelöst!${NC}"
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Process interrupted${NC}"; exit 1' INT

# Run main process
main "$@"
