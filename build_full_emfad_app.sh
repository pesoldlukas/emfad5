#!/bin/bash

# EMFAD® Full App Build & Deploy
# Baut die vollständige EMFAD App mit Jetpack Compose Frontend und Backend

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
APP_PACKAGE="com.emfad.app.debug"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"

echo -e "${BLUE}🔧 EMFAD® Full App Build & Deploy${NC}"
echo -e "${BLUE}=================================${NC}"
echo -e "Target: ${GREEN}Samsung Galaxy S21 Ultra${NC}"
echo -e "Package: ${GREEN}$APP_PACKAGE${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${BLUE}[BUILD]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_info() {
    echo -e "${CYAN}[INFO]${NC} $1"
}

# Check Samsung S21 Ultra connection
check_device() {
    print_status "Checking Samsung S21 Ultra connection..."
    
    if $ADB devices | grep -q "device$"; then
        local device_model=$($ADB shell getprop ro.product.model 2>/dev/null || echo "Unknown")
        if [[ "$device_model" == *"SM-G998"* ]]; then
            print_success "Samsung S21 Ultra connected: $device_model"
            return 0
        else
            print_warning "Connected device: $device_model (not S21 Ultra)"
            return 1
        fi
    else
        print_error "No device connected"
        return 1
    fi
}

# Create gradle wrapper
create_gradle_wrapper() {
    print_status "Creating Gradle Wrapper..."
    
    # Create gradle wrapper directory
    mkdir -p gradle/wrapper
    
    # Create gradle-wrapper.properties
    cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

    # Create gradlew script
    cat > gradlew << 'EOF'
#!/bin/sh

# Gradle start up script for UN*X

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/" ; done
    echo " "
}
APP_ARGS=`save "$@"`

# Collect all arguments for the java command
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=gradlew\"" -classpath "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$APP_ARGS"
EOF

    chmod +x gradlew
    
    # Download gradle wrapper jar
    if command -v curl &> /dev/null; then
        curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.0.0/gradle/wrapper/gradle-wrapper.jar
    elif command -v wget &> /dev/null; then
        wget -O gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.0.0/gradle/wrapper/gradle-wrapper.jar
    else
        print_warning "Neither curl nor wget available, gradle wrapper may not work"
    fi
    
    print_success "Gradle Wrapper created"
}

# Build the app
build_app() {
    print_status "Building EMFAD App with full frontend and backend..."
    
    # Clean previous builds
    print_status "Cleaning previous builds..."
    rm -rf build/
    
    # Try different build methods
    if [ -f "gradlew" ]; then
        print_status "Using Gradle Wrapper..."
        ./gradlew clean assembleDebug
    elif command -v gradle &> /dev/null; then
        print_status "Using system Gradle..."
        gradle clean assembleDebug
    else
        print_error "No Gradle build system available"
        return 1
    fi
    
    # Check if APK was created
    if [ -f "build/outputs/apk/debug/app-debug.apk" ]; then
        print_success "APK built successfully"
        return 0
    elif [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
        print_success "APK built successfully"
        return 0
    else
        print_error "APK build failed"
        return 1
    fi
}

# Install and test app
install_and_test() {
    print_status "Installing EMFAD App on Samsung S21 Ultra..."
    
    # Find APK file
    APK_FILE=""
    if [ -f "build/outputs/apk/debug/app-debug.apk" ]; then
        APK_FILE="build/outputs/apk/debug/app-debug.apk"
    elif [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
        APK_FILE="build/outputs/apk/debug/com.emfad.app-debug.apk"
    else
        print_error "APK file not found"
        return 1
    fi
    
    local apk_size=$(du -h "$APK_FILE" | cut -f1)
    print_info "APK size: $apk_size"
    
    # Install APK
    $ADB install -r "$APK_FILE"
    
    if [ $? -eq 0 ]; then
        print_success "Installation successful"
    else
        print_error "Installation failed"
        return 1
    fi
    
    # Start app
    print_status "Starting EMFAD App..."
    $ADB logcat -c  # Clear logcat
    $ADB shell am start -n "$APP_PACKAGE/.MainActivity"
    
    sleep 3
    
    # Check if app is running
    local app_pid=$($ADB shell pidof "$APP_PACKAGE" 2>/dev/null || echo "")
    
    if [ -n "$app_pid" ]; then
        print_success "EMFAD App started successfully (PID: $app_pid)"
        
        # Show recent logs
        print_info "Recent app logs:"
        $ADB logcat -d | grep -i "emfad\|timber" | tail -5
        
        return 0
    else
        print_error "App failed to start"
        
        # Show error logs
        print_info "Error logs:"
        $ADB logcat -d | grep -i "crash\|fatal\|error.*$APP_PACKAGE" | tail -10
        
        return 1
    fi
}

# Main build process
main() {
    print_status "Starting EMFAD® Full App Build & Deploy..."
    
    # Check device connection
    if ! check_device; then
        print_warning "Samsung S21 Ultra not connected, building anyway..."
    fi
    
    # Create gradle wrapper if needed
    if [ ! -f "gradlew" ]; then
        create_gradle_wrapper
    fi
    
    # Build app
    if ! build_app; then
        print_error "Build failed"
        exit 1
    fi
    
    # Install and test if device is connected
    if check_device; then
        if ! install_and_test; then
            print_error "Installation/testing failed"
            exit 1
        fi
    else
        print_warning "Device not connected, skipping installation"
    fi
    
    echo ""
    echo -e "${GREEN}🎉 EMFAD® Full App Build Completed! 🎉${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo -e "📱 App Status: ${GREEN}Built Successfully${NC}"
    echo -e "📊 Frontend: ${GREEN}Jetpack Compose${NC}"
    echo -e "🔧 Backend: ${GREEN}Integrated${NC}"
    echo -e "📄 APK: ${GREEN}Ready for Deployment${NC}"
    echo ""
    
    if check_device; then
        echo -e "${CYAN}Next Steps:${NC}"
        echo -e "1. Test app functionality on Samsung S21 Ultra"
        echo -e "2. Connect EMFAD hardware for testing"
        echo -e "3. Monitor performance during use"
        echo -e "4. Report any issues for optimization"
    else
        echo -e "${CYAN}To deploy on Samsung S21 Ultra:${NC}"
        echo -e "1. Connect Samsung S21 Ultra via USB"
        echo -e "2. Enable USB Debugging"
        echo -e "3. Run: ./deploy_samsung_s21_ultra.sh"
    fi
    
    echo ""
    echo -e "${GREEN}✅ EMFAD® App with full frontend and backend ready!${NC}"
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Build interrupted${NC}"; exit 1' INT

# Run main build
main "$@"
