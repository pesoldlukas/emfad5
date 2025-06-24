#!/bin/bash

# EMFAD® Android App - Live Testing & Debug Analysis
# Samsung S21 Ultra Optimized Testing Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
APP_PACKAGE="com.emfad.app"
MAIN_ACTIVITY="com.emfad.app.MainActivity"
TEST_DURATION=300  # 5 minutes
LOG_FILE="samsung_s21_ultra_test_$(date +%Y%m%d_%H%M%S).log"

echo -e "${BLUE}🔧 EMFAD® Live Testing & Debug Analysis${NC}"
echo -e "${BLUE}====================================${NC}"
echo -e "Target Device: ${GREEN}Samsung S21 Ultra${NC}"
echo -e "Package: ${GREEN}$APP_PACKAGE${NC}"
echo -e "Log File: ${GREEN}$LOG_FILE${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${BLUE}[TEST]${NC} $1"
    echo "[$(date '+%H:%M:%S')] $1" >> "$LOG_FILE"
}

print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    echo "[$(date '+%H:%M:%S')] PASS: $1" >> "$LOG_FILE"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
    echo "[$(date '+%H:%M:%S')] WARN: $1" >> "$LOG_FILE"
}

print_error() {
    echo -e "${RED}[FAIL]${NC} $1"
    echo "[$(date '+%H:%M:%S')] FAIL: $1" >> "$LOG_FILE"
}

print_info() {
    echo -e "${CYAN}[INFO]${NC} $1"
    echo "[$(date '+%H:%M:%S')] INFO: $1" >> "$LOG_FILE"
}

# Check if ADB is available
check_adb() {
    if ! command -v adb &> /dev/null; then
        print_error "ADB not found in PATH"
        echo "Please install Android SDK and add platform-tools to PATH"
        echo "Or use: export PATH=\$PATH:/path/to/android-sdk/platform-tools"
        exit 1
    fi
    print_success "ADB found"
}

# Check device connection
check_device_connection() {
    print_status "Checking Samsung S21 Ultra connection..."
    
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    
    if [ "$DEVICES" -eq 0 ]; then
        print_error "No Android device connected"
        echo "Please connect Samsung S21 Ultra via USB and enable USB debugging"
        exit 1
    elif [ "$DEVICES" -gt 1 ]; then
        print_warning "Multiple devices connected, using first device"
    fi
    
    # Get device info
    DEVICE_MODEL=$(adb shell getprop ro.product.model)
    DEVICE_VERSION=$(adb shell getprop ro.build.version.release)
    DEVICE_API=$(adb shell getprop ro.build.version.sdk)
    
    print_info "Device Model: $DEVICE_MODEL"
    print_info "Android Version: $DEVICE_VERSION (API $DEVICE_API)"
    
    # Check if it's Samsung S21 Ultra
    if echo "$DEVICE_MODEL" | grep -qi "SM-G998"; then
        print_success "Samsung S21 Ultra detected"
    else
        print_warning "Device is not Samsung S21 Ultra: $DEVICE_MODEL"
    fi
}

# Install or update app
install_app() {
    print_status "Installing/updating EMFAD app..."
    
    APK_PATH="build/outputs/apk/debug/app-debug.apk"
    if [ ! -f "$APK_PATH" ]; then
        print_error "Debug APK not found at $APK_PATH"
        print_status "Building debug APK..."
        ./gradlew assembleDebug
    fi
    
    adb install -r "$APK_PATH"
    if [ $? -eq 0 ]; then
        print_success "App installed successfully"
    else
        print_error "App installation failed"
        exit 1
    fi
}

# Start app and begin monitoring
start_app_monitoring() {
    print_status "Starting EMFAD app and monitoring..."
    
    # Clear logcat
    adb logcat -c
    
    # Start app
    adb shell am start -n "$APP_PACKAGE/$MAIN_ACTIVITY"
    sleep 3
    
    # Check if app started
    if adb shell pidof "$APP_PACKAGE" > /dev/null; then
        print_success "App started successfully"
    else
        print_error "App failed to start"
        return 1
    fi
}

# Monitor app performance
monitor_performance() {
    print_status "Monitoring app performance..."
    
    # Get app PID
    APP_PID=$(adb shell pidof "$APP_PACKAGE")
    
    if [ -z "$APP_PID" ]; then
        print_error "App not running"
        return 1
    fi
    
    print_info "App PID: $APP_PID"
    
    # Monitor for 30 seconds
    for i in {1..6}; do
        # Memory usage
        MEMORY=$(adb shell dumpsys meminfo "$APP_PACKAGE" | grep "TOTAL" | awk '{print $2}')
        
        # CPU usage
        CPU=$(adb shell top -p "$APP_PID" -n 1 | tail -1 | awk '{print $9}')
        
        print_info "Memory: ${MEMORY}KB, CPU: ${CPU}%"
        
        sleep 5
    done
}

# Test app functionality
test_app_functionality() {
    print_status "Testing app functionality..."
    
    # Test 1: Check if main activity is visible
    CURRENT_ACTIVITY=$(adb shell dumpsys activity activities | grep "mResumedActivity" | awk '{print $4}')
    if echo "$CURRENT_ACTIVITY" | grep -q "$APP_PACKAGE"; then
        print_success "Main activity is active"
    else
        print_warning "Main activity not in foreground: $CURRENT_ACTIVITY"
    fi
    
    # Test 2: Check for crashes
    CRASH_COUNT=$(adb logcat -d | grep -i "FATAL\|AndroidRuntime.*$APP_PACKAGE" | wc -l)
    if [ "$CRASH_COUNT" -eq 0 ]; then
        print_success "No crashes detected"
    else
        print_error "$CRASH_COUNT crashes detected"
        adb logcat -d | grep -i "FATAL\|AndroidRuntime.*$APP_PACKAGE" >> "$LOG_FILE"
    fi
    
    # Test 3: Check for ANRs
    ANR_COUNT=$(adb logcat -d | grep -i "ANR.*$APP_PACKAGE" | wc -l)
    if [ "$ANR_COUNT" -eq 0 ]; then
        print_success "No ANRs detected"
    else
        print_error "$ANR_COUNT ANRs detected"
    fi
    
    # Test 4: Check permissions
    print_status "Checking app permissions..."
    PERMISSIONS=$(adb shell dumpsys package "$APP_PACKAGE" | grep "permission\." | grep "granted=true" | wc -l)
    print_info "Granted permissions: $PERMISSIONS"
}

# Test EMFAD specific functionality
test_emfad_features() {
    print_status "Testing EMFAD-specific features..."
    
    # Test Bluetooth
    print_status "Testing Bluetooth functionality..."
    BT_ENABLED=$(adb shell settings get global bluetooth_on)
    if [ "$BT_ENABLED" = "1" ]; then
        print_success "Bluetooth is enabled"
    else
        print_warning "Bluetooth is disabled"
    fi
    
    # Test GPS
    print_status "Testing GPS functionality..."
    GPS_ENABLED=$(adb shell settings get secure location_providers_allowed)
    if echo "$GPS_ENABLED" | grep -q "gps"; then
        print_success "GPS is enabled"
    else
        print_warning "GPS is disabled"
    fi
    
    # Test Camera (for AR features)
    print_status "Testing Camera access..."
    CAMERA_PERMISSION=$(adb shell dumpsys package "$APP_PACKAGE" | grep "android.permission.CAMERA" | grep "granted=true")
    if [ -n "$CAMERA_PERMISSION" ]; then
        print_success "Camera permission granted"
    else
        print_warning "Camera permission not granted"
    fi
}

# Simulate user interactions
simulate_user_interactions() {
    print_status "Simulating user interactions..."
    
    # Tap on screen center (navigate through app)
    adb shell input tap 540 1200
    sleep 2
    
    # Swipe gestures
    adb shell input swipe 540 1200 540 800 500
    sleep 1
    
    # Back button
    adb shell input keyevent 4
    sleep 1
    
    # Menu button
    adb shell input keyevent 82
    sleep 1
    
    print_success "User interactions simulated"
}

# Collect debug logs
collect_debug_logs() {
    print_status "Collecting debug logs..."
    
    # EMFAD app logs
    adb logcat -d | grep "EMFAD" > "emfad_logs_$(date +%Y%m%d_%H%M%S).txt"
    
    # System logs related to our app
    adb logcat -d | grep "$APP_PACKAGE" > "app_logs_$(date +%Y%m%d_%H%M%S).txt"
    
    # Performance logs
    adb shell dumpsys meminfo "$APP_PACKAGE" > "memory_info_$(date +%Y%m%d_%H%M%S).txt"
    adb shell dumpsys cpuinfo | grep "$APP_PACKAGE" > "cpu_info_$(date +%Y%m%d_%H%M%S).txt"
    
    print_success "Debug logs collected"
}

# Analyze performance metrics
analyze_performance() {
    print_status "Analyzing performance metrics..."
    
    # Memory analysis
    MEMORY_TOTAL=$(adb shell dumpsys meminfo "$APP_PACKAGE" | grep "TOTAL" | awk '{print $2}')
    MEMORY_MB=$((MEMORY_TOTAL / 1024))
    
    if [ "$MEMORY_MB" -lt 200 ]; then
        print_success "Memory usage: ${MEMORY_MB}MB (Excellent)"
    elif [ "$MEMORY_MB" -lt 400 ]; then
        print_success "Memory usage: ${MEMORY_MB}MB (Good)"
    elif [ "$MEMORY_MB" -lt 600 ]; then
        print_warning "Memory usage: ${MEMORY_MB}MB (High)"
    else
        print_error "Memory usage: ${MEMORY_MB}MB (Too High)"
    fi
    
    # Battery analysis
    BATTERY_LEVEL=$(adb shell dumpsys battery | grep level | awk '{print $2}')
    print_info "Battery level: ${BATTERY_LEVEL}%"
    
    # Network usage
    NETWORK_RX=$(adb shell cat /proc/net/dev | grep wlan0 | awk '{print $2}')
    NETWORK_TX=$(adb shell cat /proc/net/dev | grep wlan0 | awk '{print $10}')
    print_info "Network RX: $NETWORK_RX bytes, TX: $NETWORK_TX bytes"
}

# Generate test report
generate_test_report() {
    print_status "Generating test report..."
    
    REPORT_FILE="samsung_s21_ultra_test_report_$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$REPORT_FILE" << EOF
# EMFAD® Samsung S21 Ultra Test Report

## Test Information
- **Date**: $(date)
- **Device**: Samsung S21 Ultra
- **App Package**: $APP_PACKAGE
- **Test Duration**: $TEST_DURATION seconds

## Device Information
- **Model**: $(adb shell getprop ro.product.model)
- **Android Version**: $(adb shell getprop ro.build.version.release)
- **API Level**: $(adb shell getprop ro.build.version.sdk)
- **RAM**: $(adb shell cat /proc/meminfo | grep MemTotal | awk '{print $2}') KB
- **CPU**: $(adb shell getprop ro.product.cpu.abi)

## Performance Metrics
- **Memory Usage**: ${MEMORY_MB}MB
- **Battery Level**: ${BATTERY_LEVEL}%
- **App PID**: $APP_PID

## Test Results
$(cat "$LOG_FILE")

## Recommendations
- Monitor memory usage during extended sessions
- Test with real EMFAD device connections
- Verify GPS accuracy in different environments
- Test Bluetooth connectivity with various devices

## Next Steps
1. Address any identified issues
2. Optimize performance bottlenecks
3. Test with real EMFAD hardware
4. Conduct user acceptance testing
EOF
    
    print_success "Test report generated: $REPORT_FILE"
}

# Main test execution
main() {
    echo "Starting EMFAD Samsung S21 Ultra Live Test..." > "$LOG_FILE"
    
    check_adb
    check_device_connection
    install_app
    start_app_monitoring
    monitor_performance
    test_app_functionality
    test_emfad_features
    simulate_user_interactions
    collect_debug_logs
    analyze_performance
    generate_test_report
    
    echo ""
    echo -e "${GREEN}🎉 Live Testing Completed Successfully! 🎉${NC}"
    echo -e "${BLUE}Check the following files for detailed results:${NC}"
    echo -e "📄 Test Log: ${GREEN}$LOG_FILE${NC}"
    echo -e "📊 Test Report: ${GREEN}$REPORT_FILE${NC}"
    echo -e "🔍 Debug Logs: ${GREEN}emfad_logs_*.txt${NC}"
    echo ""
}

# Run main function
main "$@"
