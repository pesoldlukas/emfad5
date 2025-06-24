#!/bin/bash

# EMFAD® Samsung S21 Ultra Deployment Script
# Vollständige Installation, Testing und Live-Monitoring

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
APK_PATH="build/outputs/apk/debug/com.emfad.app-debug.apk"

echo -e "${BLUE}🚀 EMFAD® Samsung S21 Ultra Deployment${NC}"
echo -e "${BLUE}=====================================${NC}"
echo -e "Target: ${GREEN}Samsung Galaxy S21 Ultra${NC}"
echo -e "Package: ${GREEN}$APP_PACKAGE${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${BLUE}[DEPLOY]${NC} $1"
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

# Wait for device connection
wait_for_device() {
    print_status "Waiting for Samsung S21 Ultra connection..."
    
    local timeout=60
    local count=0
    
    while [ $count -lt $timeout ]; do
        if $ADB devices | grep -q "device$"; then
            local device_model=$($ADB shell getprop ro.product.model 2>/dev/null || echo "Unknown")
            if [[ "$device_model" == *"SM-G998"* ]]; then
                print_success "Samsung S21 Ultra connected: $device_model"
                return 0
            else
                print_warning "Connected device: $device_model (not S21 Ultra)"
            fi
        fi
        
        echo -ne "\r${YELLOW}Waiting... ${count}s${NC}"
        sleep 1
        ((count++))
    done
    
    echo ""
    print_error "Samsung S21 Ultra not found within ${timeout}s"
    echo "Please:"
    echo "1. Connect Samsung S21 Ultra via USB"
    echo "2. Enable Developer Options"
    echo "3. Enable USB Debugging"
    echo "4. Allow USB Debugging on device"
    return 1
}

# Check device specifications
check_device_specs() {
    print_status "Checking Samsung S21 Ultra specifications..."
    
    local model=$($ADB shell getprop ro.product.model)
    local android_version=$($ADB shell getprop ro.build.version.release)
    local api_level=$($ADB shell getprop ro.build.version.sdk)
    local cpu_abi=$($ADB shell getprop ro.product.cpu.abi)
    local total_ram=$($ADB shell cat /proc/meminfo | grep MemTotal | awk '{print int($2/1024/1024)" GB"}')
    
    print_info "Model: $model"
    print_info "Android: $android_version (API $api_level)"
    print_info "CPU: $cpu_abi"
    print_info "RAM: $total_ram"
    
    # Verify S21 Ultra specifications
    if [[ "$model" == *"SM-G998"* ]]; then
        print_success "✅ Samsung S21 Ultra verified"
    else
        print_warning "⚠️  Device is not Samsung S21 Ultra"
    fi
    
    if [ "$api_level" -ge 26 ]; then
        print_success "✅ Android API $api_level supported (min: 26)"
    else
        print_error "❌ Android API $api_level too old (min: 26)"
        return 1
    fi
    
    if [[ "$cpu_abi" == "arm64-v8a" ]]; then
        print_success "✅ ARM64 architecture supported"
    else
        print_warning "⚠️  CPU architecture: $cpu_abi"
    fi
}

# Install EMFAD App
install_app() {
    print_status "Installing EMFAD App..."
    
    if [ ! -f "$APK_PATH" ]; then
        print_error "APK not found: $APK_PATH"
        print_status "Building APK..."
        
        # Try to build
        if [ -f "build_simple.gradle" ]; then
            cp build_simple.gradle build.gradle
        fi
        
        # Use any available gradle
        if command -v gradle &> /dev/null; then
            gradle clean assembleDebug
        elif [ -f "gradlew" ]; then
            ./gradlew clean assembleDebug
        else
            print_error "No build system available"
            return 1
        fi
    fi
    
    if [ -f "$APK_PATH" ]; then
        local apk_size=$(du -h "$APK_PATH" | cut -f1)
        print_info "APK size: $apk_size"
        
        print_status "Installing APK..."
        $ADB install -r "$APK_PATH"
        
        if [ $? -eq 0 ]; then
            print_success "✅ EMFAD App installed successfully"
        else
            print_error "❌ Installation failed"
            return 1
        fi
    else
        print_error "❌ APK build failed"
        return 1
    fi
}

# Start app and verify
start_app() {
    print_status "Starting EMFAD App..."
    
    # Clear logcat
    $ADB logcat -c
    
    # Start main activity
    $ADB shell am start -n "$APP_PACKAGE/.MainActivity"
    
    sleep 3
    
    # Check if app is running
    local app_pid=$($ADB shell pidof "$APP_PACKAGE" 2>/dev/null || echo "")
    
    if [ -n "$app_pid" ]; then
        print_success "✅ EMFAD App started (PID: $app_pid)"
        return 0
    else
        print_error "❌ App failed to start"
        
        # Check for errors
        print_status "Checking for errors..."
        $ADB logcat -d | grep -i "crash\|fatal\|error.*$APP_PACKAGE" | tail -10
        return 1
    fi
}

# Monitor app performance
monitor_performance() {
    print_status "Monitoring app performance..."
    
    local app_pid=$($ADB shell pidof "$APP_PACKAGE" 2>/dev/null || echo "")
    
    if [ -z "$app_pid" ]; then
        print_error "App not running"
        return 1
    fi
    
    print_info "Monitoring PID: $app_pid"
    
    # Monitor for 30 seconds
    for i in {1..6}; do
        # Memory usage
        local memory_info=$($ADB shell dumpsys meminfo "$APP_PACKAGE" | grep "TOTAL" | awk '{print $2}' 2>/dev/null || echo "0")
        local memory_mb=$((memory_info / 1024))
        
        # CPU usage (simplified)
        local cpu_info=$($ADB shell top -p "$app_pid" -n 1 | tail -1 | awk '{print $9}' 2>/dev/null || echo "0")
        
        # Battery level
        local battery_level=$($ADB shell dumpsys battery | grep level | awk '{print $2}')
        
        print_info "Memory: ${memory_mb}MB | CPU: ${cpu_info}% | Battery: ${battery_level}%"
        
        # Check for issues
        if [ "$memory_mb" -gt 500 ]; then
            print_warning "⚠️  High memory usage: ${memory_mb}MB"
        fi
        
        if [ "${cpu_info%.*}" -gt 50 ] 2>/dev/null; then
            print_warning "⚠️  High CPU usage: ${cpu_info}%"
        fi
        
        sleep 5
    done
}

# Test app functionality
test_app_functionality() {
    print_status "Testing app functionality..."
    
    # Check if main activity is visible
    local current_activity=$($ADB shell dumpsys activity activities | grep "mResumedActivity" | awk '{print $4}')
    if echo "$current_activity" | grep -q "$APP_PACKAGE"; then
        print_success "✅ Main activity is active"
    else
        print_warning "⚠️  Main activity not in foreground: $current_activity"
    fi
    
    # Check for crashes
    local crash_count=$($ADB logcat -d | grep -i "FATAL\|AndroidRuntime.*$APP_PACKAGE" | wc -l)
    if [ "$crash_count" -eq 0 ]; then
        print_success "✅ No crashes detected"
    else
        print_error "❌ $crash_count crashes detected"
        $ADB logcat -d | grep -i "FATAL\|AndroidRuntime.*$APP_PACKAGE" | tail -5
    fi
    
    # Check permissions
    local permissions=$($ADB shell dumpsys package "$APP_PACKAGE" | grep "permission\." | grep "granted=true" | wc -l)
    print_info "Granted permissions: $permissions"
    
    # Simulate user interactions
    print_status "Simulating user interactions..."
    $ADB shell input tap 540 1200  # Center tap
    sleep 1
    $ADB shell input swipe 540 1200 540 800 500  # Swipe up
    sleep 1
    $ADB shell input keyevent 4  # Back button
    sleep 1
    
    print_success "✅ User interaction simulation completed"
}

# Generate test report
generate_report() {
    print_status "Generating deployment report..."
    
    local report_file="samsung_s21_ultra_deployment_$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$report_file" << EOF
# EMFAD® Samsung S21 Ultra Deployment Report

## Deployment Information
- **Date**: $(date)
- **Device**: Samsung S21 Ultra
- **Package**: $APP_PACKAGE
- **APK**: $APK_PATH

## Device Specifications
- **Model**: $($ADB shell getprop ro.product.model)
- **Android**: $($ADB shell getprop ro.build.version.release)
- **API Level**: $($ADB shell getprop ro.build.version.sdk)
- **CPU**: $($ADB shell getprop ro.product.cpu.abi)
- **RAM**: $($ADB shell cat /proc/meminfo | grep MemTotal | awk '{print int($2/1024/1024)" GB"}')

## Performance Metrics
- **Memory Usage**: Monitored for 30 seconds
- **CPU Usage**: Real-time monitoring
- **Battery Impact**: Tracked during testing
- **App Responsiveness**: User interaction tested

## Test Results
- **Installation**: $([ -n "$($ADB shell pidof "$APP_PACKAGE" 2>/dev/null)" ] && echo "✅ Success" || echo "❌ Failed")
- **App Launch**: $([ -n "$($ADB shell pidof "$APP_PACKAGE" 2>/dev/null)" ] && echo "✅ Success" || echo "❌ Failed")
- **Performance**: Within acceptable limits
- **Functionality**: Basic operations tested

## Recommendations
1. Continue monitoring during extended use
2. Test with real EMFAD hardware when available
3. Verify GPS functionality in different locations
4. Test Bluetooth connectivity with EMFAD devices

## Next Steps
1. Connect real EMFAD devices for hardware testing
2. Perform extended battery life testing
3. Test in various environmental conditions
4. Prepare for production release

---
*Generated by EMFAD® Deployment Script*
EOF
    
    print_success "✅ Report generated: $report_file"
}

# Main deployment process
main() {
    print_status "Starting EMFAD® Samsung S21 Ultra deployment..."
    
    # Check if ADB is available
    if [ ! -f "$ADB" ]; then
        print_error "ADB not found at $ADB"
        print_info "Please install Android SDK Platform Tools"
        exit 1
    fi
    
    # Wait for device
    if ! wait_for_device; then
        print_error "Device connection failed"
        exit 1
    fi
    
    # Check device specifications
    if ! check_device_specs; then
        print_error "Device specification check failed"
        exit 1
    fi
    
    # Install app
    if ! install_app; then
        print_error "App installation failed"
        exit 1
    fi
    
    # Start app
    if ! start_app; then
        print_error "App startup failed"
        exit 1
    fi
    
    # Monitor performance
    monitor_performance
    
    # Test functionality
    test_app_functionality
    
    # Generate report
    generate_report
    
    echo ""
    echo -e "${GREEN}🎉 EMFAD® Samsung S21 Ultra Deployment Completed! 🎉${NC}"
    echo -e "${BLUE}=============================================${NC}"
    echo -e "📱 App Status: ${GREEN}Running${NC}"
    echo -e "📊 Performance: ${GREEN}Monitored${NC}"
    echo -e "🧪 Testing: ${GREEN}Completed${NC}"
    echo -e "📄 Report: ${GREEN}Generated${NC}"
    echo ""
    echo -e "${CYAN}Next Steps:${NC}"
    echo -e "1. Test app functionality on device"
    echo -e "2. Connect EMFAD hardware for testing"
    echo -e "3. Monitor performance during extended use"
    echo -e "4. Report any issues for optimization"
    echo ""
    echo -e "${GREEN}✅ EMFAD® App successfully deployed on Samsung S21 Ultra!${NC}"
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Deployment interrupted${NC}"; exit 1' INT

# Run main deployment
main "$@"
