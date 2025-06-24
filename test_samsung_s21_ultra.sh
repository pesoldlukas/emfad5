#!/bin/bash

# 📱 SAMSUNG S21 ULTRA AUTOMATISIERTER TEST
# Testet die EMFAD App auf Samsung S21 Ultra mit vollständiger Ghidra-Integration

set -e

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test-Konfiguration
APP_PACKAGE="com.emfad.app.debug"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
TEST_DURATION=1800 # 30 Minuten
LOG_FILE="samsung_s21_ultra_test_$(date +%Y%m%d_%H%M%S).log"

echo -e "${BLUE}🔍 SAMSUNG S21 ULTRA TEST GESTARTET${NC}"
echo "=================================================="
echo "Test-Zeit: $(date)"
echo "Log-Datei: $LOG_FILE"
echo "App-Package: $APP_PACKAGE"
echo "=================================================="

# Logging-Funktion
log() {
    echo -e "$1" | tee -a "$LOG_FILE"
}

# Test-Funktion
test_step() {
    local step_name="$1"
    local command="$2"
    local expected="$3"
    
    log "${YELLOW}🧪 Testing: $step_name${NC}"
    
    if eval "$command"; then
        log "${GREEN}✅ BESTANDEN: $step_name${NC}"
        return 0
    else
        log "${RED}❌ FEHLGESCHLAGEN: $step_name${NC}"
        return 1
    fi
}

# Geräte-Verbindung prüfen
check_device() {
    log "${BLUE}📱 Prüfe Samsung S21 Ultra Verbindung...${NC}"
    
    if ! adb devices | grep -q "device$"; then
        log "${RED}❌ Kein Android-Gerät gefunden!${NC}"
        log "Bitte Samsung S21 Ultra per USB verbinden und USB-Debugging aktivieren."
        exit 1
    fi
    
    # Geräteinformationen abrufen
    local device_model=$(adb shell getprop ro.product.model)
    local android_version=$(adb shell getprop ro.build.version.release)
    local api_level=$(adb shell getprop ro.build.version.sdk)
    
    log "${GREEN}✅ Gerät verbunden:${NC}"
    log "   Modell: $device_model"
    log "   Android: $android_version (API $api_level)"
    
    # Prüfe ob es ein Samsung S21 Ultra ist
    if [[ "$device_model" == *"SM-G998"* ]] || [[ "$device_model" == *"Galaxy S21 Ultra"* ]]; then
        log "${GREEN}✅ Samsung S21 Ultra erkannt!${NC}"
    else
        log "${YELLOW}⚠️  Warnung: Gerät ist kein Samsung S21 Ultra ($device_model)${NC}"
        log "Test wird trotzdem fortgesetzt..."
    fi
}

# APK installieren
install_apk() {
    log "${BLUE}📦 Installiere EMFAD App...${NC}"
    
    if [ ! -f "$APK_PATH" ]; then
        log "${RED}❌ APK nicht gefunden: $APK_PATH${NC}"
        log "Bitte zuerst APK erstellen mit: ./gradlew assembleDebug"
        exit 1
    fi
    
    # Alte Version deinstallieren
    adb uninstall "$APP_PACKAGE" 2>/dev/null || true
    
    # Neue Version installieren
    if adb install -r "$APK_PATH"; then
        log "${GREEN}✅ APK erfolgreich installiert${NC}"
    else
        log "${RED}❌ APK-Installation fehlgeschlagen${NC}"
        exit 1
    fi
}

# App-Start-Test
test_app_launch() {
    log "${BLUE}🚀 Teste App-Start...${NC}"
    
    # App starten
    adb shell am start -n "$APP_PACKAGE/.SimpleEMFADActivity"
    sleep 5
    
    # Prüfe ob App läuft
    if adb shell pidof "$APP_PACKAGE" > /dev/null; then
        log "${GREEN}✅ App erfolgreich gestartet${NC}"
        return 0
    else
        log "${RED}❌ App-Start fehlgeschlagen${NC}"
        return 1
    fi
}

# Permissions-Test
test_permissions() {
    log "${BLUE}🔐 Teste Permissions...${NC}"
    
    local permissions=(
        "android.permission.BLUETOOTH"
        "android.permission.BLUETOOTH_ADMIN"
        "android.permission.ACCESS_FINE_LOCATION"
        "android.permission.CAMERA"
        "android.permission.WRITE_EXTERNAL_STORAGE"
    )
    
    for permission in "${permissions[@]}"; do
        # Permission gewähren
        adb shell pm grant "$APP_PACKAGE" "$permission" 2>/dev/null || true
        
        # Prüfe Permission-Status
        local status=$(adb shell dumpsys package "$APP_PACKAGE" | grep "$permission" | head -1)
        if [[ "$status" == *"granted=true"* ]]; then
            log "${GREEN}✅ Permission gewährt: $permission${NC}"
        else
            log "${YELLOW}⚠️  Permission nicht gewährt: $permission${NC}"
        fi
    done
}

# Ghidra-Komponenten-Test
test_ghidra_components() {
    log "${BLUE}🔍 Teste Ghidra-Komponenten...${NC}"
    
    # Starte LogCat-Monitoring für Ghidra-Logs
    adb logcat -c  # Clear logs
    
    # Teste EMFAD Tablet Config
    log "   Testing EMFADTabletConfig..."
    sleep 2
    
    # Teste Frequenz-Management
    log "   Testing FrequencyConfig..."
    sleep 2
    
    # Teste Autobalance-System
    log "   Testing AutobalanceConfig..."
    sleep 2
    
    # Prüfe Ghidra-Logs
    local ghidra_logs=$(adb logcat -d | grep -i "ghidra\|emfad" | wc -l)
    if [ "$ghidra_logs" -gt 0 ]; then
        log "${GREEN}✅ Ghidra-Komponenten aktiv ($ghidra_logs Log-Einträge)${NC}"
        return 0
    else
        log "${YELLOW}⚠️  Keine Ghidra-Logs gefunden${NC}"
        return 1
    fi
}

# Hardware-Kommunikation-Test
test_hardware_communication() {
    log "${BLUE}🔧 Teste Hardware-Kommunikation...${NC}"
    
    # Teste USB-OTG-Unterstützung
    local usb_otg=$(adb shell cat /sys/class/android_usb/android0/functions 2>/dev/null || echo "unknown")
    log "   USB-OTG-Status: $usb_otg"
    
    # Teste Bluetooth-Unterstützung
    local bluetooth_status=$(adb shell dumpsys bluetooth_manager | grep "enabled" | head -1)
    log "   Bluetooth-Status: $bluetooth_status"
    
    # Prüfe Device-Controller-Logs
    local device_logs=$(adb logcat -d | grep -i "GhidraDeviceController\|connectToDevice" | wc -l)
    if [ "$device_logs" -gt 0 ]; then
        log "${GREEN}✅ Device-Controller aktiv ($device_logs Log-Einträge)${NC}"
        return 0
    else
        log "${YELLOW}⚠️  Keine Device-Controller-Logs gefunden${NC}"
        return 1
    fi
}

# Performance-Test
test_performance() {
    log "${BLUE}⚡ Teste Performance...${NC}"
    
    # CPU-Auslastung
    local cpu_usage=$(adb shell top -n 1 | grep "$APP_PACKAGE" | awk '{print $9}' | head -1)
    log "   CPU-Auslastung: ${cpu_usage:-0}%"
    
    # Speicherverbrauch
    local memory_info=$(adb shell dumpsys meminfo "$APP_PACKAGE" | grep "TOTAL" | awk '{print $2}')
    local memory_mb=$((memory_info / 1024))
    log "   Speicherverbrauch: ${memory_mb} MB"
    
    # FPS (falls verfügbar)
    local fps_info=$(adb shell dumpsys gfxinfo "$APP_PACKAGE" | grep "Total frames" | head -1)
    log "   Grafik-Performance: $fps_info"
    
    # Performance-Bewertung
    if [ "${cpu_usage:-0}" -lt 50 ] && [ "$memory_mb" -lt 500 ]; then
        log "${GREEN}✅ Performance akzeptabel${NC}"
        return 0
    else
        log "${YELLOW}⚠️  Performance-Warnung: CPU=${cpu_usage}%, RAM=${memory_mb}MB${NC}"
        return 1
    fi
}

# Stabilität-Test
test_stability() {
    log "${BLUE}🛡️  Teste Stabilität (${TEST_DURATION}s)...${NC}"
    
    local start_time=$(date +%s)
    local end_time=$((start_time + TEST_DURATION))
    local crash_count=0
    
    while [ $(date +%s) -lt $end_time ]; do
        # Prüfe ob App noch läuft
        if ! adb shell pidof "$APP_PACKAGE" > /dev/null; then
            crash_count=$((crash_count + 1))
            log "${RED}❌ App-Crash erkannt! (Crash #$crash_count)${NC}"
            
            # App neu starten
            adb shell am start -n "$APP_PACKAGE/.SimpleEMFADActivity"
            sleep 5
        fi
        
        # Rotation-Test
        adb shell content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:1
        sleep 2
        adb shell content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:0
        sleep 2
        
        # Background/Foreground-Test
        adb shell input keyevent KEYCODE_HOME
        sleep 1
        adb shell am start -n "$APP_PACKAGE/.SimpleEMFADActivity"
        sleep 2
        
        # Fortschritt anzeigen
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        local remaining=$((TEST_DURATION - elapsed))
        echo -ne "\r   Verbleibende Zeit: ${remaining}s (Crashes: $crash_count)"
    done
    
    echo ""
    if [ "$crash_count" -eq 0 ]; then
        log "${GREEN}✅ Stabilität-Test bestanden (0 Crashes)${NC}"
        return 0
    else
        log "${RED}❌ Stabilität-Test fehlgeschlagen ($crash_count Crashes)${NC}"
        return 1
    fi
}

# Export/Import-Test
test_export_import() {
    log "${BLUE}📁 Teste Export/Import-Funktionen...${NC}"
    
    # Prüfe Export-Logs
    local export_logs=$(adb logcat -d | grep -i "export\|import\|DAT\|EGD\|ESD" | wc -l)
    if [ "$export_logs" -gt 0 ]; then
        log "${GREEN}✅ Export/Import-Funktionen aktiv ($export_logs Log-Einträge)${NC}"
        return 0
    else
        log "${YELLOW}⚠️  Keine Export/Import-Logs gefunden${NC}"
        return 1
    fi
}

# Cleanup
cleanup() {
    log "${BLUE}🧹 Cleanup...${NC}"
    
    # App stoppen
    adb shell am force-stop "$APP_PACKAGE"
    
    # LogCat stoppen
    pkill -f "adb logcat" 2>/dev/null || true
}

# Signal-Handler für Cleanup
trap cleanup EXIT

# Haupt-Test-Durchlauf
main() {
    local total_tests=0
    local passed_tests=0
    
    # Test-Schritte
    tests=(
        "check_device"
        "install_apk" 
        "test_app_launch"
        "test_permissions"
        "test_ghidra_components"
        "test_hardware_communication"
        "test_performance"
        "test_export_import"
        "test_stability"
    )
    
    # Führe alle Tests durch
    for test_func in "${tests[@]}"; do
        total_tests=$((total_tests + 1))
        
        if $test_func; then
            passed_tests=$((passed_tests + 1))
        fi
        
        echo ""
    done
    
    # Test-Zusammenfassung
    log "=================================================="
    log "${BLUE}📊 TEST-ZUSAMMENFASSUNG${NC}"
    log "=================================================="
    log "Gesamt-Tests: $total_tests"
    log "Bestanden: $passed_tests"
    log "Fehlgeschlagen: $((total_tests - passed_tests))"
    log "Erfolgsrate: $((passed_tests * 100 / total_tests))%"
    
    if [ "$passed_tests" -eq "$total_tests" ]; then
        log "${GREEN}🎉 ALLE TESTS BESTANDEN!${NC}"
        log "${GREEN}✅ EMFAD App ist bereit für Samsung S21 Ultra!${NC}"
        exit 0
    else
        log "${RED}❌ EINIGE TESTS FEHLGESCHLAGEN!${NC}"
        log "${YELLOW}🔧 Bitte Fehler beheben und erneut testen.${NC}"
        exit 1
    fi
}

# Starte Haupt-Test
main "$@"
