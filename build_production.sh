#!/bin/bash

# EMFAD® Android App - Production Build Script
# Optimiert für Samsung S21 Ultra Deployment

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Build Configuration
APP_NAME="EMFAD"
VERSION_NAME="1.0.0"
BUILD_TYPE="release"
TARGET_DEVICE="Samsung S21 Ultra"

echo -e "${BLUE}🚀 EMFAD® Android App - Production Build${NC}"
echo -e "${BLUE}=======================================${NC}"
echo -e "App: ${GREEN}$APP_NAME${NC}"
echo -e "Version: ${GREEN}$VERSION_NAME${NC}"
echo -e "Build Type: ${GREEN}$BUILD_TYPE${NC}"
echo -e "Target: ${GREEN}$TARGET_DEVICE${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
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

# Check prerequisites
print_status "Checking prerequisites..."

# Check if Android SDK is available
if ! command -v adb &> /dev/null; then
    print_error "Android SDK not found. Please install Android SDK and add to PATH."
    exit 1
fi

# Check if Gradle wrapper exists
if [ ! -f "./gradlew" ]; then
    print_error "Gradle wrapper not found. Please run from project root directory."
    exit 1
fi

# Check if keystore exists (for release signing)
KEYSTORE_PATH="keystore/release.keystore"
if [ ! -f "$KEYSTORE_PATH" ]; then
    print_warning "Release keystore not found at $KEYSTORE_PATH"
    print_warning "Using debug keystore for signing"
fi

print_success "Prerequisites check completed"

# Clean previous builds
print_status "Cleaning previous builds..."
./gradlew clean
print_success "Clean completed"

# Run tests
print_status "Running unit tests..."
./gradlew testDebugUnitTest
if [ $? -eq 0 ]; then
    print_success "Unit tests passed"
else
    print_error "Unit tests failed"
    exit 1
fi

# Run lint checks
print_status "Running lint checks..."
./gradlew lintRelease
if [ $? -eq 0 ]; then
    print_success "Lint checks passed"
else
    print_warning "Lint checks found issues (continuing build)"
fi

# Build Release APK
print_status "Building Release APK..."
./gradlew assembleRelease
if [ $? -eq 0 ]; then
    print_success "Release APK build completed"
else
    print_error "Release APK build failed"
    exit 1
fi

# Build Release AAB (Android App Bundle)
print_status "Building Release AAB..."
./gradlew bundleRelease
if [ $? -eq 0 ]; then
    print_success "Release AAB build completed"
else
    print_error "Release AAB build failed"
    exit 1
fi

# Verify build outputs
print_status "Verifying build outputs..."

APK_PATH="build/outputs/apk/release/app-release.apk"
AAB_PATH="build/outputs/bundle/release/app-release.aab"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    print_success "APK found: $APK_PATH ($APK_SIZE)"
else
    print_error "APK not found at expected location"
    exit 1
fi

if [ -f "$AAB_PATH" ]; then
    AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
    print_success "AAB found: $AAB_PATH ($AAB_SIZE)"
else
    print_error "AAB not found at expected location"
    exit 1
fi

# APK Analysis
print_status "Analyzing APK..."
if command -v aapt &> /dev/null; then
    echo -e "${BLUE}APK Information:${NC}"
    aapt dump badging "$APK_PATH" | grep -E "(package|sdkVersion|targetSdkVersion|application-label)"
    echo ""
fi

# Check APK size
APK_SIZE_BYTES=$(stat -f%z "$APK_PATH" 2>/dev/null || stat -c%s "$APK_PATH" 2>/dev/null)
APK_SIZE_MB=$((APK_SIZE_BYTES / 1024 / 1024))

if [ $APK_SIZE_MB -gt 100 ]; then
    print_warning "APK size is ${APK_SIZE_MB}MB (consider optimization)"
else
    print_success "APK size is ${APK_SIZE_MB}MB (good)"
fi

# Samsung S21 Ultra specific checks
print_status "Performing Samsung S21 Ultra compatibility checks..."

# Check for arm64-v8a support
if aapt list "$APK_PATH" | grep -q "lib/arm64-v8a"; then
    print_success "arm64-v8a libraries found (Samsung S21 Ultra compatible)"
else
    print_warning "arm64-v8a libraries not found"
fi

# Check for required permissions
print_status "Checking required permissions..."
REQUIRED_PERMISSIONS=(
    "android.permission.BLUETOOTH_CONNECT"
    "android.permission.ACCESS_FINE_LOCATION"
    "android.permission.CAMERA"
)

for permission in "${REQUIRED_PERMISSIONS[@]}"; do
    if aapt dump permissions "$APK_PATH" | grep -q "$permission"; then
        print_success "Permission found: $permission"
    else
        print_warning "Permission missing: $permission"
    fi
done

# Create deployment package
print_status "Creating deployment package..."
DEPLOY_DIR="deploy_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$DEPLOY_DIR"

# Copy build artifacts
cp "$APK_PATH" "$DEPLOY_DIR/emfad-v${VERSION_NAME}-release.apk"
cp "$AAB_PATH" "$DEPLOY_DIR/emfad-v${VERSION_NAME}-release.aab"

# Copy mapping file (if exists)
MAPPING_FILE="build/outputs/mapping/release/mapping.txt"
if [ -f "$MAPPING_FILE" ]; then
    cp "$MAPPING_FILE" "$DEPLOY_DIR/mapping.txt"
    print_success "ProGuard mapping file included"
fi

# Create installation script
cat > "$DEPLOY_DIR/install_samsung_s21_ultra.sh" << 'EOF'
#!/bin/bash
echo "Installing EMFAD App on Samsung S21 Ultra..."

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "Error: No Android device connected"
    echo "Please connect Samsung S21 Ultra via USB and enable USB debugging"
    exit 1
fi

# Install APK
echo "Installing APK..."
adb install -r emfad-v1.0.0-release.apk

if [ $? -eq 0 ]; then
    echo "✅ EMFAD App successfully installed!"
    echo "You can now launch the app from the device"
else
    echo "❌ Installation failed"
    exit 1
fi
EOF

chmod +x "$DEPLOY_DIR/install_samsung_s21_ultra.sh"

# Create README for deployment
cat > "$DEPLOY_DIR/README.md" << EOF
# EMFAD® Android App - Deployment Package

## Build Information
- **Version**: $VERSION_NAME
- **Build Date**: $(date)
- **Target Device**: Samsung S21 Ultra
- **Build Type**: Release

## Files Included
- \`emfad-v${VERSION_NAME}-release.apk\` - Android APK for direct installation
- \`emfad-v${VERSION_NAME}-release.aab\` - Android App Bundle for Play Store
- \`mapping.txt\` - ProGuard mapping file for crash analysis
- \`install_samsung_s21_ultra.sh\` - Installation script

## Installation Instructions

### Samsung S21 Ultra Direct Installation
1. Enable Developer Options and USB Debugging on device
2. Connect device via USB
3. Run: \`./install_samsung_s21_ultra.sh\`

### Manual Installation
\`\`\`bash
adb install -r emfad-v${VERSION_NAME}-release.apk
\`\`\`

### Play Store Deployment
Upload \`emfad-v${VERSION_NAME}-release.aab\` to Google Play Console

## App Features
- ✅ USB-Serial + Bluetooth BLE Communication
- ✅ 7 EMFAD Frequencies (19-135.6 kHz)
- ✅ Real-time Signal Analysis & Depth Calculation
- ✅ GPS Tracking with OpenStreetMap
- ✅ AutoBalance Calibration System
- ✅ Export/Import (EGD/ESD/FADS/DAT formats)
- ✅ Material 3 Design optimized for Samsung S21 Ultra

## System Requirements
- Android 8.0+ (API 26+)
- Samsung S21 Ultra recommended
- 4GB+ RAM
- Bluetooth 5.0+
- GPS capability
- USB Host support (for EMFAD devices)

## Support
For technical support, contact the EMFAD development team.
EOF

print_success "Deployment package created: $DEPLOY_DIR"

# Final summary
echo ""
echo -e "${GREEN}🎉 BUILD COMPLETED SUCCESSFULLY! 🎉${NC}"
echo -e "${BLUE}=================================${NC}"
echo -e "📱 APK: ${GREEN}emfad-v${VERSION_NAME}-release.apk${NC} (${APK_SIZE})"
echo -e "📦 AAB: ${GREEN}emfad-v${VERSION_NAME}-release.aab${NC} (${AAB_SIZE})"
echo -e "📁 Deploy: ${GREEN}$DEPLOY_DIR${NC}"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo -e "1. Test APK on Samsung S21 Ultra device"
echo -e "2. Upload AAB to Google Play Console"
echo -e "3. Distribute APK for beta testing"
echo ""
echo -e "${GREEN}✅ EMFAD Android App is ready for production deployment!${NC}"
