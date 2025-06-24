#!/bin/bash

# EMFAD® Original Windows UI - Samsung S21 Ultra Deployment
# Exakte Nachbildung der originalen EMFAD Windows Software

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
APP_PACKAGE="com.emfad.app.debug"

echo -e "${BLUE}🔧 EMFAD® Original Windows UI - Samsung S21 Ultra${NC}"
echo -e "${BLUE}===================================================${NC}"
echo -e "${CYAN}Exakte Nachbildung der originalen EMFAD Windows Software${NC}"
echo ""

# ASCII Art EMFAD Logo
echo -e "${MAGENTA}"
echo "███████╗███╗   ███╗███████╗ █████╗ ██████╗ "
echo "██╔════╝████╗ ████║██╔════╝██╔══██╗██╔══██╗"
echo "█████╗  ██╔████╔██║█████╗  ███████║██║  ██║"
echo "██╔══╝  ██║╚██╔╝██║██╔══╝  ██╔══██║██║  ██║"
echo "███████╗██║ ╚═╝ ██║██║     ██║  ██║██████╔╝"
echo "╚══════╝╚═╝     ╚═╝╚═╝     ╚═╝  ╚═╝╚═════╝ "
echo -e "${NC}"
echo -e "${CYAN}Original Windows UI für Android${NC}"
echo ""

# Check Samsung S21 Ultra
check_device() {
    echo -e "${BLUE}📱 Checking Samsung S21 Ultra connection...${NC}"
    
    if $ADB devices | grep -q "device$"; then
        local device_model=$($ADB shell getprop ro.product.model 2>/dev/null || echo "Unknown")
        local device_id=$($ADB devices | grep "device$" | awk '{print $1}')
        
        if [[ "$device_model" == *"SM-G998"* ]]; then
            echo -e "${GREEN}✅ Samsung S21 Ultra connected${NC}"
            echo -e "   Model: ${GREEN}$device_model${NC}"
            echo -e "   Device ID: ${GREEN}$device_id${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  Connected device: $device_model${NC}"
            echo -e "${CYAN}   Continuing with deployment...${NC}"
            return 0
        fi
    else
        echo -e "${RED}❌ No device connected${NC}"
        echo -e "${YELLOW}Please connect Samsung S21 Ultra via USB${NC}"
        return 1
    fi
}

# Show original EMFAD Windows UI features
show_original_features() {
    echo -e "${BLUE}🎨 Original EMFAD Windows UI Features:${NC}"
    echo -e "${GREEN}✅ EMFAD Header${NC} - Black logo with red accent lines"
    echo -e "${GREEN}✅ EMFAD EMUNI${NC} - Blue subtitle"
    echo -e "${GREEN}✅ Cyan Gradient Background${NC} - Royal Blue to Deep Sky Blue"
    echo -e "${GREEN}✅ 4x3 Function Grid${NC} - White cards with blue icons"
    echo -e "${GREEN}✅ Function Buttons:${NC}"
    echo "   • assign COM    • profile       • scan GPS      • connect"
    echo "   • tools         • spectrum      • path          • AR"
    echo "   • setup         • scan 2D/3D    • map           • EMTOMO"
    echo -e "${GREEN}✅ Bottom Control Bar${NC} - close application, antenna A, parallel, filter 1"
    echo ""
}

# Install and test original UI
install_original_ui() {
    echo -e "${BLUE}📲 Installing Original EMFAD UI...${NC}"
    
    # Check if app is installed
    local installed_app=$($ADB shell pm list packages | grep "com.emfad.app" || echo "")
    
    if [ -n "$installed_app" ]; then
        echo -e "${GREEN}✅ EMFAD App found${NC}"
        
        # Force stop current app
        echo -e "${BLUE}🛑 Stopping current app...${NC}"
        $ADB shell am force-stop com.emfad.app.debug 2>/dev/null || true
        $ADB shell am force-stop com.emfad.app 2>/dev/null || true
        
        sleep 2
        
        # Clear logs
        $ADB logcat -c
        
        # Start with original UI
        echo -e "${BLUE}🚀 Starting Original EMFAD Windows UI...${NC}"
        
        # Try both package names
        if $ADB shell am start -n com.emfad.app.debug/.MainActivity 2>/dev/null; then
            echo -e "${GREEN}✅ Started with debug package${NC}"
        elif $ADB shell am start -n com.emfad.app/.MainActivity 2>/dev/null; then
            echo -e "${GREEN}✅ Started with release package${NC}"
        else
            echo -e "${RED}❌ Failed to start app${NC}"
            return 1
        fi
        
        sleep 3
        
        # Check if app started
        local app_pid=$($ADB shell pidof com.emfad.app.debug 2>/dev/null || $ADB shell pidof com.emfad.app 2>/dev/null || echo "")
        
        if [ -n "$app_pid" ]; then
            echo -e "${GREEN}✅ Original EMFAD UI started successfully (PID: $app_pid)${NC}"
            
            # Show recent logs
            echo -e "${BLUE}📋 App startup logs:${NC}"
            $ADB logcat -d | grep -i "emfad\|timber\|MainActivity" | tail -5
            
            echo ""
            echo -e "${GREEN}🎉 Original EMFAD Windows UI läuft auf Samsung S21 Ultra!${NC}"
            echo -e "${CYAN}📱 Prüfe das Gerät für das originale EMFAD Frontend${NC}"
            
            # Show what user should see
            show_expected_ui

            return 0
        else
            echo -e "${RED}❌ App failed to start${NC}"
            
            # Show error logs
            echo -e "${BLUE}📋 Error logs:${NC}"
            $ADB logcat -d | grep -i "crash\|fatal\|error" | tail -10
            
            return 1
        fi
    else
        echo -e "${YELLOW}⚠️  EMFAD App not installed${NC}"
        echo -e "${CYAN}📱 Please build and install the app first${NC}"
        echo ""
        echo -e "${BLUE}Build commands:${NC}"
        echo "  gradle clean assembleDebug"
        echo "  adb install -r build/outputs/apk/debug/com.emfad.app-debug.apk"
        return 1
    fi
}

# Show expected UI
show_expected_ui() {
    echo -e "${CYAN}📱 Expected UI on Samsung S21 Ultra:${NC}"
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
    echo -e "${BLUE}│${NC}  tools   ${BLUE}│${NC} spectrum ${BLUE}│${NC}   path   ${BLUE}│${NC}    AR    ${BLUE}│${NC}"
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
}

# Test UI functionality
test_original_ui() {
    echo -e "${BLUE}🧪 Testing Original UI functionality...${NC}"
    
    # Simulate taps on function cards
    echo -e "${CYAN}📱 Simulating function card taps...${NC}"
    
    # Tap connect button (top right)
    echo "   Tapping 'connect' button..."
    $ADB shell input tap 810 400
    sleep 1
    
    # Tap spectrum button (middle left)
    echo "   Tapping 'spectrum' button..."
    $ADB shell input tap 270 600
    sleep 1
    
    # Tap map button (bottom right-middle)
    echo "   Tapping 'map' button..."
    $ADB shell input tap 540 800
    sleep 1
    
    # Tap AR button (middle right)
    echo "   Tapping 'AR' button..."
    $ADB shell input tap 810 600
    sleep 1

    # Tap EMTOMO button (bottom right)
    echo "   Tapping 'EMTOMO' button..."
    $ADB shell input tap 810 800
    sleep 1
    
    echo -e "${GREEN}✅ UI interaction test completed${NC}"
}

# Monitor performance
monitor_performance() {
    echo -e "${BLUE}📊 Monitoring Samsung S21 Ultra performance...${NC}"
    
    for i in {1..3}; do
        local app_pid=$($ADB shell pidof com.emfad.app.debug 2>/dev/null || $ADB shell pidof com.emfad.app 2>/dev/null || echo "")
        
        if [ -n "$app_pid" ]; then
            # Memory usage
            local memory_info=$($ADB shell dumpsys meminfo com.emfad.app.debug | grep "TOTAL" | awk '{print $2}' 2>/dev/null || echo "0")
            local memory_mb=$((memory_info / 1024))
            
            # CPU usage (simplified)
            local cpu_info=$($ADB shell top -n 1 | grep "com.emfad.app" | awk '{print $9}' | head -1 2>/dev/null || echo "0")
            
            # Battery level
            local battery_level=$($ADB shell dumpsys battery | grep level | awk '{print $2}' 2>/dev/null || echo "Unknown")
            
            echo -e "   [${i}/3] Memory: ${GREEN}${memory_mb}MB${NC} | CPU: ${GREEN}${cpu_info}%${NC} | Battery: ${GREEN}${battery_level}%${NC} | PID: $app_pid"
            
            if [ "$memory_mb" -gt 300 ]; then
                echo -e "   ${YELLOW}⚠️  Memory usage higher than expected${NC}"
            fi
        else
            echo -e "   [${i}/3] ${RED}App not running${NC}"
        fi
        
        sleep 2
    done
}

# Main process
main() {
    echo -e "${BLUE}Starting Original EMFAD Windows UI Deployment...${NC}"
    echo ""
    
    # Show features
    show_original_features
    
    # Check device
    if ! check_device; then
        exit 1
    fi
    
    echo ""
    
    # Install and test
    if ! install_original_ui; then
        exit 1
    fi
    
    echo ""
    
    # Test functionality
    test_original_ui
    
    echo ""
    
    # Monitor performance
    monitor_performance
    
    echo ""
    echo -e "${GREEN}🎉 Original EMFAD Windows UI Deployment Complete! 🎉${NC}"
    echo -e "${BLUE}======================================================${NC}"
    echo -e "📱 Status: ${GREEN}Running on Samsung S21 Ultra${NC}"
    echo -e "🎨 UI: ${GREEN}Original EMFAD Windows Design${NC}"
    echo -e "🔧 Features: ${GREEN}4x3 Function Grid${NC}"
    echo -e "📊 Performance: ${GREEN}Optimized for S21 Ultra${NC}"
    echo ""
    echo -e "${CYAN}Das originale EMFAD Windows UI läuft jetzt auf Android!${NC}"
    echo -e "${YELLOW}Teste alle Funktions-Buttons auf dem Gerät${NC}"
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Deployment interrupted${NC}"; exit 1' INT

# Run main process
main "$@"
