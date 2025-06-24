# 📱 SAMSUNG S21 ULTRA TEST-ANLEITUNG

## 🎯 EMFAD APP TESTING AUF SAMSUNG S21 ULTRA

### 📋 VORBEREITUNG

#### 1. **Samsung S21 Ultra Spezifikationen**
- **Modell**: Samsung Galaxy S21 Ultra 5G (SM-G998B)
- **Android Version**: Android 11+ (empfohlen: Android 13+)
- **RAM**: 12GB/16GB
- **Prozessor**: Exynos 2100 / Snapdragon 888
- **USB**: USB-C 3.2 mit OTG-Unterstützung
- **Bluetooth**: 5.2 mit BLE-Unterstützung

#### 2. **Entwickleroptionen aktivieren**
```bash
# Auf dem Samsung S21 Ultra:
1. Einstellungen → Telefoninfo → Software-Informationen
2. 7x auf "Build-Nummer" tippen
3. Entwickleroptionen → USB-Debugging aktivieren
4. Entwickleroptionen → OEM-Entsperrung aktivieren (falls erforderlich)
```

#### 3. **ADB-Verbindung testen**
```bash
# Auf dem Computer:
adb devices
# Sollte das Samsung S21 Ultra anzeigen
```

### 🔧 APK-BUILD FÜR SAMSUNG S21 ULTRA

#### 1. **Debug-Build erstellen**
```bash
cd /Volumes/PortableSSD/emfad3/com.emfad.app

# Gradle Wrapper verwenden (falls vorhanden)
./gradlew assembleDebug

# Oder mit Android Studio
# Build → Build Bundle(s) / APK(s) → Build APK(s)
```

#### 2. **APK auf Samsung S21 Ultra installieren**
```bash
# APK-Datei finden
find . -name "*.apk" -type f

# APK installieren
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Oder über USB-Übertragung und manuelle Installation
```

### 🧪 SYSTEMATISCHE TEST-DURCHFÜHRUNG

#### **Phase 1: App-Start und Grundfunktionen** ⏱️ 10 Min

1. **App-Launch-Test**
   ```
   ✅ App startet ohne Crash
   ✅ Splash-Screen wird angezeigt
   ✅ Hauptmenü lädt korrekt
   ✅ Navigation funktioniert
   ```

2. **UI-Responsiveness**
   ```
   ✅ Jetpack Compose UI rendert korrekt
   ✅ Material 3 Design wird angezeigt
   ✅ Touch-Eingaben reagieren
   ✅ Animationen laufen flüssig
   ```

3. **Permissions-Test**
   ```
   ✅ Bluetooth-Permission wird angefordert
   ✅ Camera-Permission wird angefordert (für AR)
   ✅ Storage-Permission wird angefordert
   ✅ Location-Permission wird angefordert (für GPS)
   ```

#### **Phase 2: Ghidra-Komponenten-Tests** ⏱️ 15 Min

4. **EMFAD Tablet Config Test**
   ```
   ✅ EMFADTabletConfig lädt "EMFAD TABLET 1.0"
   ✅ Scan-Modi werden angezeigt (2D/3D, DS, Line, Profile)
   ✅ Konfiguration wird gespeichert
   ```

5. **Frequenz-Management Test**
   ```
   ✅ Alle 7 EMFAD-Frequenzen werden angezeigt:
       - f0: 19.0 KHz ✅
       - f1: 23.4 KHz ✅
       - f2: 70.0 KHz ✅
       - f3: 77.5 KHz ✅
       - f4: 124.0 KHz ✅
       - f5: 129.1 KHz ✅
       - f6: 135.6 KHz ✅
   ✅ Frequenzauswahl funktioniert
   ✅ TfrmFrequencyModeSelect Dialog öffnet
   ```

6. **Autobalance-System Test**
   ```
   ✅ AutobalanceConfig lädt "autobalance values; version 1.0"
   ✅ TfrmAutoBalance Dialog öffnet
   ✅ Kalibrierungs-Stati werden angezeigt:
       - Compass calibration ✅
       - Horizontal calibration ✅
       - Vertical calibration ✅
   ✅ Kalibrierungs-Buttons reagieren
   ```

#### **Phase 3: Hardware-Kommunikation** ⏱️ 20 Min

7. **USB-Serial Test**
   ```
   ✅ USB-OTG wird erkannt
   ✅ FTDI-Adapter wird erkannt (falls vorhanden)
   ✅ Prolific-Adapter wird erkannt (falls vorhanden)
   ✅ Silicon Labs-Adapter wird erkannt (falls vorhanden)
   ✅ GhidraDeviceController.formCreate() funktioniert
   ✅ Device-Status zeigt "no port" oder "device connected"
   ```

8. **Bluetooth-Kommunikation Test**
   ```
   ✅ BLE-Scan startet
   ✅ EMFAD-Geräte werden erkannt
   ✅ Verbindung zu EMFAD-Gerät möglich
   ✅ Nordic BLE Library funktioniert
   ✅ Bluetooth-Fallback aktiviert sich
   ```

9. **Geräte-Protokoll Test**
   ```
   ✅ EMFAD-Sync-Byte (0xAA) wird gesendet
   ✅ Kommandos werden übertragen:
       - EMFAD_CMD_STATUS ✅
       - EMFAD_CMD_START ✅
       - EMFAD_CMD_FREQ ✅
       - EMFAD_CMD_CALIBRATE ✅
   ✅ Antworten werden empfangen
   ```

#### **Phase 4: Datenverarbeitung** ⏱️ 15 Min

10. **Fortran-Processor Test**
    ```
    ✅ GhidraFortranProcessor.readlineUn() funktioniert
    ✅ GhidraFortranProcessor.readlineF() funktioniert
    ✅ Array-Bounds-Checking funktioniert
    ✅ EMF-Datenverarbeitung läuft
    ✅ Komplexe Zahlen-Verarbeitung funktioniert
    ```

11. **Messdaten-Verarbeitung Test**
    ```
    ✅ EMFReading-Objekte werden erstellt
    ✅ Tiefenberechnung mit Kalibrierungskonstante 3333
    ✅ Real/Imaginär-Teil-Berechnung
    ✅ Magnitude und Phase-Berechnung
    ✅ Qualitätsscore-Berechnung
    ```

12. **MeasurementService Integration**
    ```
    ✅ Service startet ohne Fehler
    ✅ Ghidra-Komponenten werden initialisiert
    ✅ Messsitzung kann gestartet werden
    ✅ Daten werden verarbeitet und gespeichert
    ✅ Service kann gestoppt werden
    ```

#### **Phase 5: Export/Import-Funktionen** ⏱️ 10 Min

13. **Export-Test**
    ```
    ✅ exportDAT1Click() funktioniert
    ✅ export2D1Click() funktioniert
    ✅ EGD-Format-Export funktioniert
    ✅ ESD-Format-Export funktioniert
    ✅ Dateien werden im korrekten Format erstellt
    ```

14. **Import-Test**
    ```
    ✅ importTabletFile1Click() funktioniert
    ✅ Datenvalidierung funktioniert
    ✅ "Used frequency;" wird erkannt
    ✅ "No frequency set in file." wird angezeigt
    ✅ Fehlerhafte Daten werden erkannt
    ```

#### **Phase 6: Performance und Stabilität** ⏱️ 10 Min

15. **Performance-Test**
    ```
    ✅ App läuft flüssig bei 60 FPS
    ✅ Speicherverbrauch unter 500 MB
    ✅ CPU-Auslastung unter 30%
    ✅ Batterieverbrauch akzeptabel
    ✅ Keine Memory Leaks (LeakCanary)
    ```

16. **Stabilität-Test**
    ```
    ✅ App läuft 30 Minuten ohne Crash
    ✅ Rotation funktioniert korrekt
    ✅ Background/Foreground-Wechsel funktioniert
    ✅ Multitasking funktioniert
    ✅ Keine ANR (Application Not Responding)
    ```

### 📊 TEST-PROTOKOLL

#### **Test-Umgebung**
- **Gerät**: Samsung Galaxy S21 Ultra
- **Android Version**: _____________
- **App Version**: 1.0.0-debug
- **Test-Datum**: _____________
- **Tester**: _____________

#### **Test-Ergebnisse**
```
Phase 1 - App-Start:           ✅ BESTANDEN / ❌ FEHLGESCHLAGEN
Phase 2 - Ghidra-Komponenten: ✅ BESTANDEN / ❌ FEHLGESCHLAGEN  
Phase 3 - Hardware-Komm.:     ✅ BESTANDEN / ❌ FEHLGESCHLAGEN
Phase 4 - Datenverarbeitung:  ✅ BESTANDEN / ❌ FEHLGESCHLAGEN
Phase 5 - Export/Import:      ✅ BESTANDEN / ❌ FEHLGESCHLAGEN
Phase 6 - Performance:        ✅ BESTANDEN / ❌ FEHLGESCHLAGEN

GESAMT-BEWERTUNG: ✅ BESTANDEN / ❌ FEHLGESCHLAGEN
```

#### **Gefundene Probleme**
```
1. Problem: ________________________________
   Schweregrad: KRITISCH / HOCH / MITTEL / NIEDRIG
   Status: OFFEN / BEHOBEN

2. Problem: ________________________________
   Schweregrad: KRITISCH / HOCH / MITTEL / NIEDRIG
   Status: OFFEN / BEHOBEN

3. Problem: ________________________________
   Schweregrad: KRITISCH / HOCH / MITTEL / NIEDRIG
   Status: OFFEN / BEHOBEN
```

### 🔧 DEBUGGING-HILFEN

#### **LogCat-Filter für EMFAD**
```bash
adb logcat | grep -E "(EMFAD|Ghidra|MeasurementService)"
```

#### **Wichtige Log-Tags**
```
- EMFADMeasurementService
- GhidraDeviceController  
- GhidraExportImport
- GhidraFortranProcessor
- EMFADBluetoothManager
- MaterialClassifier
```

#### **Performance-Monitoring**
```bash
# CPU-Auslastung
adb shell top | grep com.emfad.app

# Speicherverbrauch
adb shell dumpsys meminfo com.emfad.app

# Batterieverbrauch
adb shell dumpsys batterystats | grep com.emfad.app
```

### 🚀 NÄCHSTE SCHRITTE

Nach erfolgreichem Testing:

1. **Release-Build erstellen**
2. **Code-Signing für Distribution**
3. **Performance-Optimierungen**
4. **Dokumentation aktualisieren**
5. **User-Manual erstellen**

**Die EMFAD App ist bereit für das Samsung S21 Ultra Testing!** 📱✅
