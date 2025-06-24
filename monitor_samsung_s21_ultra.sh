#!/bin/bash

# EMFAD® Samsung S21 Ultra Live Monitoring
# Real-time performance and debug monitoring

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
APP_PACKAGE="com.emfad.app"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
MONITOR_INTERVAL=5
LOG_FILE="samsung_s21_ultra_monitor_$(date +%Y%m%d_%H%M%S).log"

echo -e "${BLUE}📱 Samsung S21 Ultra Live Monitor${NC}"
echo -e "${BLUE}================================${NC}"
echo -e "Package: ${GREEN}$APP_PACKAGE${NC}"
echo -e "Interval: ${GREEN}${MONITOR_INTERVAL}s${NC}"
echo -e "Log: ${GREEN}$LOG_FILE${NC}"
echo ""

# Check device connection
check_device() {
    if ! $ADB devices | grep -q "device$"; then
        echo -e "${RED}❌ Samsung S21 Ultra not connected${NC}"
        exit 1
    fi
    
    DEVICE_MODEL=$($ADB shell getprop ro.product.model)
    if [[ "$DEVICE_MODEL" == *"SM-G998"* ]]; then
        echo -e "${GREEN}✅ Samsung S21 Ultra connected: $DEVICE_MODEL${NC}"
    else
        echo -e "${YELLOW}⚠️  Connected device: $DEVICE_MODEL${NC}"
    fi
}

# Get device info
get_device_info() {
    echo -e "${CYAN}📱 Device Information:${NC}"
    echo "Model: $($ADB shell getprop ro.product.model)"
    echo "Android: $($ADB shell getprop ro.build.version.release)"
    echo "API Level: $($ADB shell getprop ro.build.version.sdk)"
    echo "CPU: $($ADB shell getprop ro.product.cpu.abi)"
    echo "RAM: $($ADB shell cat /proc/meminfo | grep MemTotal | awk '{print $2/1024/1024 " GB"}')"
    echo ""
}

# Monitor app if running
monitor_app() {
    APP_PID=$($ADB shell pidof "$APP_PACKAGE" 2>/dev/null || echo "")
    
    if [ -z "$APP_PID" ]; then
        echo -e "${YELLOW}⏸️  EMFAD App not running${NC}"
        return
    fi
    
    echo -e "${GREEN}🚀 EMFAD App running (PID: $APP_PID)${NC}"
    
    # Memory usage
    MEMORY_INFO=$($ADB shell dumpsys meminfo "$APP_PACKAGE" | grep "TOTAL" | awk '{print $2}')
    MEMORY_MB=$((MEMORY_INFO / 1024))
    
    # CPU usage (simplified)
    CPU_INFO=$($ADB shell top -p "$APP_PID" -n 1 | tail -1 | awk '{print $9}' 2>/dev/null || echo "0")
    
    # Battery level
    BATTERY_LEVEL=$($ADB shell dumpsys battery | grep level | awk '{print $2}')
    
    # Display info
    echo -e "${BLUE}📊 Performance Metrics:${NC}"
    echo "Memory: ${MEMORY_MB}MB"
    echo "CPU: ${CPU_INFO}%"
    echo "Battery: ${BATTERY_LEVEL}%"
    
    # Log to file
    echo "[$(date '+%H:%M:%S')] Memory: ${MEMORY_MB}MB, CPU: ${CPU_INFO}%, Battery: ${BATTERY_LEVEL}%" >> "$LOG_FILE"
    
    # Check for issues
    if [ "$MEMORY_MB" -gt 500 ]; then
        echo -e "${RED}⚠️  High memory usage: ${MEMORY_MB}MB${NC}"
    fi
    
    if [ "${CPU_INFO%.*}" -gt 50 ] 2>/dev/null; then
        echo -e "${RED}⚠️  High CPU usage: ${CPU_INFO}%${NC}"
    fi
}

# Monitor system resources
monitor_system() {
    echo -e "${PURPLE}🔧 System Resources:${NC}"
    
    # Available memory
    AVAILABLE_MEM=$($ADB shell cat /proc/meminfo | grep MemAvailable | awk '{print $2/1024/1024 " GB"}')
    echo "Available RAM: $AVAILABLE_MEM"
    
    # CPU temperature (if available)
    CPU_TEMP=$($ADB shell cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null | awk '{print $1/1000 "°C"}' || echo "N/A")
    echo "CPU Temp: $CPU_TEMP"
    
    # Storage space
    STORAGE=$($ADB shell df /data | tail -1 | awk '{print "Used: " $3 ", Available: " $4}')
    echo "Storage: $STORAGE"
}

# Check for crashes
check_crashes() {
    CRASH_COUNT=$($ADB logcat -d | grep -i "FATAL\|AndroidRuntime.*$APP_PACKAGE" | wc -l)
    if [ "$CRASH_COUNT" -gt 0 ]; then
        echo -e "${RED}💥 Crashes detected: $CRASH_COUNT${NC}"
        $ADB logcat -d | grep -i "FATAL\|AndroidRuntime.*$APP_PACKAGE" | tail -5
    fi
}

# Check EMFAD specific logs
check_emfad_logs() {
    EMFAD_LOGS=$($ADB logcat -d | grep "EMFAD" | tail -5)
    if [ -n "$EMFAD_LOGS" ]; then
        echo -e "${CYAN}🔧 Recent EMFAD Logs:${NC}"
        echo "$EMFAD_LOGS"
    fi
}

# Test app functionality
test_app_functionality() {
    if [ -n "$($ADB shell pidof "$APP_PACKAGE")" ]; then
        echo -e "${BLUE}🧪 Testing App Functionality:${NC}"
        
        # Check if main activity is visible
        CURRENT_ACTIVITY=$($ADB shell dumpsys activity activities | grep "mResumedActivity" | awk '{print $4}')
        if echo "$CURRENT_ACTIVITY" | grep -q "$APP_PACKAGE"; then
            echo "✅ Main activity active"
        else
            echo "⚠️  Main activity not in foreground"
        fi
        
        # Check permissions
        PERMISSIONS=$($ADB shell dumpsys package "$APP_PACKAGE" | grep "permission\." | grep "granted=true" | wc -l)
        echo "✅ Granted permissions: $PERMISSIONS"
        
        # Simulate user interaction
        echo "🖱️  Simulating user interaction..."
        $ADB shell input tap 540 1200
        sleep 1
    fi
}

# Main monitoring loop
main_monitor() {
    echo "Starting live monitoring... (Press Ctrl+C to stop)"
    echo ""
    
    while true; do
        clear
        echo -e "${BLUE}📱 Samsung S21 Ultra Live Monitor - $(date)${NC}"
        echo -e "${BLUE}================================================${NC}"
        echo ""
        
        get_device_info
        monitor_app
        echo ""
        monitor_system
        echo ""
        check_crashes
        echo ""
        check_emfad_logs
        echo ""
        test_app_functionality
        
        echo ""
        echo -e "${YELLOW}Next update in ${MONITOR_INTERVAL}s... (Ctrl+C to stop)${NC}"
        
        sleep $MONITOR_INTERVAL
    done
}

# Install and start app if not running
install_and_start() {
    echo -e "${BLUE}🚀 Installing and starting EMFAD App...${NC}"
    
    # Check if APK exists
    APK_PATH="build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "Installing APK..."
        $ADB install -r "$APK_PATH"
        
        echo "Starting app..."
        $ADB shell am start -n "$APP_PACKAGE/.MainActivity"
        
        sleep 3
        echo -e "${GREEN}✅ App started${NC}"
    else
        echo -e "${YELLOW}⚠️  APK not found. Build first with: ./gradlew assembleDebug${NC}"
    fi
}

# Cleanup function
cleanup() {
    echo ""
    echo -e "${BLUE}🔄 Monitoring stopped${NC}"
    echo -e "📄 Log saved: ${GREEN}$LOG_FILE${NC}"
    exit 0
}

# Trap Ctrl+C
trap cleanup INT

# Check arguments
case "${1:-monitor}" in
    "install")
        check_device
        install_and_start
        ;;
    "monitor")
        check_device
        main_monitor
        ;;
    "test")
        check_device
        test_app_functionality
        ;;
    *)
        echo "Usage: $0 [install|monitor|test]"
        echo "  install - Install and start the app"
        echo "  monitor - Start live monitoring (default)"
        echo "  test    - Run functionality tests"
        ;;
esac
