#!/bin/bash

# EMFAD® Live Monitoring Dashboard
# Real-time Samsung S21 Ultra Performance Monitoring

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
UPDATE_INTERVAL=2
LOG_FILE="live_monitor_$(date +%Y%m%d_%H%M%S).log"

# Dashboard functions
clear_screen() {
    clear
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║                    🔧 EMFAD® Live Monitoring Dashboard                        ║${NC}"
    echo -e "${BLUE}║                        Samsung S21 Ultra Real-time Monitor                   ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

get_device_info() {
    if $ADB devices | grep -q "device$"; then
        local model=$($ADB shell getprop ro.product.model 2>/dev/null || echo "Unknown")
        local android=$($ADB shell getprop ro.build.version.release 2>/dev/null || echo "Unknown")
        local api=$($ADB shell getprop ro.build.version.sdk 2>/dev/null || echo "Unknown")
        local battery=$($ADB shell dumpsys battery | grep level | awk '{print $2}' 2>/dev/null || echo "Unknown")
        
        echo -e "${CYAN}📱 Device Information:${NC}"
        echo -e "   Model: ${GREEN}$model${NC}"
        echo -e "   Android: ${GREEN}$android (API $api)${NC}"
        echo -e "   Battery: ${GREEN}$battery%${NC}"
        
        if [[ "$model" == *"SM-G998"* ]]; then
            echo -e "   Status: ${GREEN}✅ Samsung S21 Ultra Verified${NC}"
        else
            echo -e "   Status: ${YELLOW}⚠️  Not Samsung S21 Ultra${NC}"
        fi
    else
        echo -e "${RED}❌ No device connected${NC}"
        return 1
    fi
}

get_app_status() {
    local app_pid=$($ADB shell pidof "$APP_PACKAGE" 2>/dev/null || echo "")
    
    echo -e "${PURPLE}🚀 EMFAD App Status:${NC}"
    
    if [ -n "$app_pid" ]; then
        echo -e "   Status: ${GREEN}✅ Running (PID: $app_pid)${NC}"
        
        # Get current activity
        local current_activity=$($ADB shell dumpsys activity activities | grep "mResumedActivity" | awk '{print $4}' 2>/dev/null || echo "Unknown")
        if echo "$current_activity" | grep -q "$APP_PACKAGE"; then
            echo -e "   Activity: ${GREEN}✅ In Foreground${NC}"
        else
            echo -e "   Activity: ${YELLOW}⚠️  In Background${NC}"
        fi
        
        # Check for recent crashes
        local crash_count=$($ADB logcat -d -t 100 | grep -i "FATAL\|AndroidRuntime.*$APP_PACKAGE" | wc -l)
        if [ "$crash_count" -eq 0 ]; then
            echo -e "   Stability: ${GREEN}✅ No Recent Crashes${NC}"
        else
            echo -e "   Stability: ${RED}❌ $crash_count Recent Crashes${NC}"
        fi
        
        return 0
    else
        echo -e "   Status: ${RED}❌ Not Running${NC}"
        echo -e "   Activity: ${RED}❌ Not Active${NC}"
        echo -e "   Stability: ${YELLOW}⚠️  App Not Started${NC}"
        return 1
    fi
}

get_performance_metrics() {
    local app_pid=$($ADB shell pidof "$APP_PACKAGE" 2>/dev/null || echo "")
    
    echo -e "${BLUE}📊 Performance Metrics:${NC}"
    
    if [ -n "$app_pid" ]; then
        # Memory usage
        local memory_info=$($ADB shell dumpsys meminfo "$APP_PACKAGE" | grep "TOTAL" | awk '{print $2}' 2>/dev/null || echo "0")
        local memory_mb=$((memory_info / 1024))
        
        # CPU usage (simplified)
        local cpu_info=$($ADB shell top -p "$app_pid" -n 1 | tail -1 | awk '{print $9}' 2>/dev/null || echo "0")
        
        # Memory status
        if [ "$memory_mb" -lt 200 ]; then
            echo -e "   Memory: ${GREEN}$memory_mb MB (Excellent)${NC}"
        elif [ "$memory_mb" -lt 400 ]; then
            echo -e "   Memory: ${GREEN}$memory_mb MB (Good)${NC}"
        elif [ "$memory_mb" -lt 600 ]; then
            echo -e "   Memory: ${YELLOW}$memory_mb MB (High)${NC}"
        else
            echo -e "   Memory: ${RED}$memory_mb MB (Critical)${NC}"
        fi
        
        # CPU status
        if [ "${cpu_info%.*}" -lt 20 ] 2>/dev/null; then
            echo -e "   CPU: ${GREEN}$cpu_info% (Excellent)${NC}"
        elif [ "${cpu_info%.*}" -lt 40 ] 2>/dev/null; then
            echo -e "   CPU: ${GREEN}$cpu_info% (Good)${NC}"
        elif [ "${cpu_info%.*}" -lt 60 ] 2>/dev/null; then
            echo -e "   CPU: ${YELLOW}$cpu_info% (High)${NC}"
        else
            echo -e "   CPU: ${RED}$cpu_info% (Critical)${NC}"
        fi
        
        # System memory
        local available_mem=$($ADB shell cat /proc/meminfo | grep MemAvailable | awk '{print int($2/1024/1024)}' 2>/dev/null || echo "Unknown")
        echo -e "   System RAM: ${CYAN}${available_mem} GB Available${NC}"
        
        # Log metrics
        echo "[$(date '+%H:%M:%S')] Memory: ${memory_mb}MB, CPU: ${cpu_info}%, Available RAM: ${available_mem}GB" >> "$LOG_FILE"
        
    else
        echo -e "   Memory: ${RED}❌ App Not Running${NC}"
        echo -e "   CPU: ${RED}❌ App Not Running${NC}"
        echo -e "   System RAM: ${CYAN}$(($($ADB shell cat /proc/meminfo | grep MemAvailable | awk '{print $2}' 2>/dev/null || echo "0") / 1024 / 1024)) GB Available${NC}"
    fi
}

get_system_status() {
    echo -e "${YELLOW}🔧 System Status:${NC}"
    
    # Battery info
    local battery_level=$($ADB shell dumpsys battery | grep level | awk '{print $2}' 2>/dev/null || echo "Unknown")
    local battery_temp=$($ADB shell dumpsys battery | grep temperature | awk '{print $2/10}' 2>/dev/null || echo "Unknown")
    
    if [ "$battery_level" != "Unknown" ]; then
        if [ "$battery_level" -gt 50 ]; then
            echo -e "   Battery: ${GREEN}$battery_level% (${battery_temp}°C)${NC}"
        elif [ "$battery_level" -gt 20 ]; then
            echo -e "   Battery: ${YELLOW}$battery_level% (${battery_temp}°C)${NC}"
        else
            echo -e "   Battery: ${RED}$battery_level% (${battery_temp}°C)${NC}"
        fi
    else
        echo -e "   Battery: ${RED}❌ Unknown${NC}"
    fi
    
    # Storage info
    local storage_info=$($ADB shell df /data | tail -1 | awk '{print "Used: " int($3/1024/1024) "GB, Available: " int($4/1024/1024) "GB"}' 2>/dev/null || echo "Unknown")
    echo -e "   Storage: ${CYAN}$storage_info${NC}"
    
    # Network status
    local wifi_status=$($ADB shell dumpsys wifi | grep "mWifiEnabled" | awk '{print $2}' 2>/dev/null || echo "Unknown")
    if [ "$wifi_status" = "true" ]; then
        echo -e "   WiFi: ${GREEN}✅ Connected${NC}"
    else
        echo -e "   WiFi: ${YELLOW}⚠️  Disconnected${NC}"
    fi
}

get_recent_logs() {
    echo -e "${CYAN}📋 Recent EMFAD Logs:${NC}"
    
    local recent_logs=$($ADB logcat -d -t 5 | grep -i "emfad\|MainActivity" 2>/dev/null || echo "No recent logs")
    
    if [ "$recent_logs" != "No recent logs" ]; then
        echo "$recent_logs" | while read -r line; do
            if echo "$line" | grep -qi "error\|crash\|fatal"; then
                echo -e "   ${RED}$line${NC}"
            elif echo "$line" | grep -qi "warn"; then
                echo -e "   ${YELLOW}$line${NC}"
            else
                echo -e "   ${GREEN}$line${NC}"
            fi
        done
    else
        echo -e "   ${YELLOW}No recent EMFAD logs${NC}"
    fi
}

show_controls() {
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ Controls: [R]efresh | [Q]uit | [L]ogs | [I]nstall | [S]tart | [K]ill         ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
}

# Main monitoring loop
main_monitor() {
    echo "Starting EMFAD® Live Monitoring Dashboard..." > "$LOG_FILE"
    echo "Log file: $LOG_FILE"
    
    while true; do
        clear_screen
        
        # Display all information
        get_device_info
        echo ""
        get_app_status
        echo ""
        get_performance_metrics
        echo ""
        get_system_status
        echo ""
        get_recent_logs
        
        show_controls
        
        # Wait for input or timeout
        read -t $UPDATE_INTERVAL -n 1 input || input=""
        
        case "$input" in
            [Qq])
                echo -e "\n${BLUE}Monitoring stopped${NC}"
                echo -e "📄 Log saved: ${GREEN}$LOG_FILE${NC}"
                exit 0
                ;;
            [Rr])
                continue
                ;;
            [Ll])
                echo -e "\n${BLUE}📋 Full log output:${NC}"
                $ADB logcat -d | grep -i "emfad" | tail -20
                read -p "Press Enter to continue..."
                ;;
            [Ii])
                echo -e "\n${BLUE}📲 Installing EMFAD App...${NC}"
                if [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
                    $ADB install -r "build/outputs/apk/debug/com.emfad.app-debug.apk"
                else
                    echo -e "${RED}❌ APK not found${NC}"
                fi
                read -p "Press Enter to continue..."
                ;;
            [Ss])
                echo -e "\n${BLUE}🚀 Starting EMFAD App...${NC}"
                $ADB shell am start -n "$APP_PACKAGE/.MainActivity"
                sleep 2
                ;;
            [Kk])
                echo -e "\n${BLUE}🛑 Stopping EMFAD App...${NC}"
                $ADB shell am force-stop "$APP_PACKAGE"
                sleep 1
                ;;
        esac
    done
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Monitoring interrupted${NC}"; echo "📄 Log saved: $LOG_FILE"; exit 0' INT

# Check if ADB is available
if [ ! -f "$ADB" ]; then
    echo -e "${RED}❌ ADB not found at $ADB${NC}"
    echo "Please install Android SDK Platform Tools"
    exit 1
fi

# Start monitoring
echo -e "${GREEN}🚀 Starting EMFAD® Live Monitoring Dashboard${NC}"
echo -e "📄 Logging to: ${CYAN}$LOG_FILE${NC}"
echo -e "⏱️  Update interval: ${CYAN}${UPDATE_INTERVAL}s${NC}"
echo ""
echo "Press any key to start monitoring..."
read -n 1

main_monitor
