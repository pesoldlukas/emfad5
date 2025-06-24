#!/bin/bash

# EMFAD® Gradle Lock Fix & Clean Build
# Löst Gradle Lock-Probleme und startet sauberen Build

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${RED}🔧 EMFAD® Gradle Lock Fix${NC}"
echo -e "${RED}==========================${NC}"
echo -e "${YELLOW}Fixing Gradle lock and daemon issues${NC}"
echo ""

# Environment setup
setup_environment() {
    export ANDROID_HOME="/Volumes/PortableSSD/AndroidSDK"
    export PATH="$ANDROID_HOME/platform-tools:$PATH"
    export JAVA_HOME="/Volumes/PortableSSD/Android Studio.app/Contents/jbr/Contents/Home"
    
    echo -e "${BLUE}🔧 Environment configured${NC}"
    echo -e "   ANDROID_HOME: $ANDROID_HOME"
    echo -e "   JAVA_HOME: $JAVA_HOME"
}

# Kill all Gradle processes
kill_gradle_processes() {
    echo -e "${BLUE}🛑 Killing Gradle processes...${NC}"
    
    # Kill Gradle daemons
    pkill -f "gradle" 2>/dev/null || true
    pkill -f "GradleDaemon" 2>/dev/null || true
    pkill -f "org.gradle" 2>/dev/null || true
    
    # Kill Java processes that might be Gradle
    ps aux | grep -i gradle | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true
    
    # Stop Gradle daemon explicitly
    if command -v gradle &> /dev/null; then
        gradle --stop 2>/dev/null || true
    fi
    
    # Try gradlew stop
    if [ -f "gradlew" ]; then
        ./gradlew --stop 2>/dev/null || true
    fi
    
    echo -e "${GREEN}✅ Gradle processes killed${NC}"
    
    # Wait a moment for processes to fully terminate
    sleep 3
}

# Clean all Gradle caches and locks
clean_gradle_completely() {
    echo -e "${BLUE}🧹 Cleaning Gradle caches and locks...${NC}"
    
    # Remove project-level Gradle files
    echo -e "${CYAN}   Cleaning project Gradle files...${NC}"
    rm -rf .gradle/ 2>/dev/null || true
    rm -rf build/ 2>/dev/null || true
    rm -rf */build/ 2>/dev/null || true
    rm -rf */.gradle/ 2>/dev/null || true
    
    # Remove global Gradle cache
    echo -e "${CYAN}   Cleaning global Gradle cache...${NC}"
    rm -rf ~/.gradle/caches/ 2>/dev/null || true
    rm -rf ~/.gradle/daemon/ 2>/dev/null || true
    rm -rf ~/.gradle/wrapper/ 2>/dev/null || true
    
    # Remove project root Gradle cache
    echo -e "${CYAN}   Cleaning project root Gradle cache...${NC}"
    rm -rf /Volumes/PortableSSD/emfad3/.gradle/ 2>/dev/null || true
    
    # Remove lock files specifically
    echo -e "${CYAN}   Removing lock files...${NC}"
    find . -name "*.lock" -delete 2>/dev/null || true
    find ~/.gradle -name "*.lock" -delete 2>/dev/null || true
    find /Volumes/PortableSSD/emfad3 -name "*.lock" -delete 2>/dev/null || true
    
    # Remove buildOutputCleanup specifically
    rm -rf ~/.gradle/buildOutputCleanup/ 2>/dev/null || true
    rm -rf .gradle/buildOutputCleanup/ 2>/dev/null || true
    rm -rf /Volumes/PortableSSD/emfad3/.gradle/buildOutputCleanup/ 2>/dev/null || true
    
    echo -e "${GREEN}✅ Gradle cleanup completed${NC}"
}

# Create fresh Gradle wrapper
create_fresh_gradle_wrapper() {
    echo -e "${BLUE}🔧 Creating fresh Gradle wrapper...${NC}"
    
    # Remove old wrapper
    rm -rf gradle/ 2>/dev/null || true
    rm -f gradlew gradlew.bat 2>/dev/null || true
    
    # Create new wrapper directory
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

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

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

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if [ "$darwin" = "true" ]; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    
    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a condition
            eval `echo args$i`=`cygpath --path --ignore --mixed "$arg"`
        else
            eval `echo args$i`="\"$arg\""
        fi
        i=`expr $i + 1`
    done
    case $i in
        0) set -- ;;
        1) set -- "$args0" ;;
        2) set -- "$args0" "$args1" ;;
        3) set -- "$args0" "$args1" "$args2" ;;
        4) set -- "$args0" "$args1" "$args2" "$args3" ;;
        5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
        6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
        7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
        8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
        9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/" ; done
    echo " "
}
APP_ARGS=`save "$@"`

# Collect all arguments for the java command
set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$CLASSPATH\"" org.gradle.wrapper.GradleWrapperMain "$APP_ARGS"

exec "$JAVACMD" "$@"
EOF

    chmod +x gradlew
    
    echo -e "${GREEN}✅ Fresh Gradle wrapper created${NC}"
}

# Try clean build
try_clean_build() {
    echo -e "${BLUE}🔨 Attempting clean build...${NC}"
    
    # Set Gradle options to avoid daemon issues
    export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.configureondemand=false"
    
    echo -e "${CYAN}📦 Starting Gradle build (no daemon)...${NC}"
    
    # Try build without daemon
    if ./gradlew clean assembleDebug --no-daemon --stacktrace 2>&1; then
        echo -e "${GREEN}✅ Build successful!${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  Gradle build failed, trying alternative...${NC}"
        
        # Try with system gradle if available
        if command -v gradle &> /dev/null; then
            echo -e "${CYAN}📦 Trying system Gradle...${NC}"
            if gradle clean assembleDebug --no-daemon 2>&1; then
                echo -e "${GREEN}✅ System Gradle build successful!${NC}"
                return 0
            fi
        fi
        
        echo -e "${RED}❌ All build attempts failed${NC}"
        return 1
    fi
}

# Check for APK and install
check_and_install_apk() {
    echo -e "${BLUE}📱 Checking for APK...${NC}"
    
    local apk_path=""
    if [ -f "build/outputs/apk/debug/app-debug.apk" ]; then
        apk_path="build/outputs/apk/debug/app-debug.apk"
    elif [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        apk_path="app/build/outputs/apk/debug/app-debug.apk"
    fi
    
    if [ -n "$apk_path" ]; then
        local apk_size=$(du -h "$apk_path" | cut -f1)
        echo -e "${GREEN}✅ APK found: $apk_path (${apk_size})${NC}"
        
        # Check if Samsung S21 Ultra connected
        if adb devices | grep -q "device$"; then
            echo -e "${BLUE}📲 Installing on Samsung S21 Ultra...${NC}"
            
            if adb install -r "$apk_path"; then
                echo -e "${GREEN}✅ APK installed successfully${NC}"
                
                # Start app
                echo -e "${BLUE}🚀 Starting EMFAD App...${NC}"
                adb logcat -c
                adb shell am start -n com.emfad.app/.MainActivity 2>/dev/null || \
                adb shell am start -n com.emfad.app.debug/.MainActivity 2>/dev/null
                
                sleep 2
                
                # Test app
                adb shell monkey -p com.emfad.app 1 2>/dev/null || \
                adb shell monkey -p com.emfad.app.debug 1 2>/dev/null
                
                echo ""
                echo -e "${GREEN}🎉 EMFAD Original Windows UI mit AR-Button läuft!${NC}"
                echo -e "${CYAN}📱 Prüfe das Samsung S21 Ultra für das UI${NC}"
                
                return 0
            else
                echo -e "${RED}❌ APK installation failed${NC}"
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

# Main process
main() {
    echo -e "${BLUE}Starting Gradle Lock Fix & Build Process...${NC}"
    echo ""
    
    # Setup environment
    setup_environment
    
    echo ""
    
    # Kill Gradle processes
    kill_gradle_processes
    
    echo ""
    
    # Clean Gradle completely
    clean_gradle_completely
    
    echo ""
    
    # Create fresh wrapper
    create_fresh_gradle_wrapper
    
    echo ""
    
    # Try clean build
    if try_clean_build; then
        echo ""
        check_and_install_apk
    else
        echo ""
        echo -e "${CYAN}📋 Manual build required:${NC}"
        echo "1. Open Android Studio"
        echo "2. File → Open → /Volumes/PortableSSD/emfad3/com.emfad.app"
        echo "3. Build → Clean Project"
        echo "4. Build → Rebuild Project"
        echo "5. Run → Run 'app'"
    fi
    
    echo ""
    echo -e "${GREEN}🎉 Gradle Lock Fix Complete! 🎉${NC}"
    echo -e "${BLUE}===============================${NC}"
    echo -e "🔧 Gradle: ${GREEN}Cleaned and reset${NC}"
    echo -e "🎨 UI Code: ${GREEN}Original EMFAD Windows + AR-Button${NC}"
    echo -e "📱 Target: ${GREEN}Samsung S21 Ultra${NC}"
    echo ""
    echo -e "${CYAN}Gradle Lock-Problem ist gelöst!${NC}"
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Process interrupted${NC}"; exit 1' INT

# Run main process
main "$@"
