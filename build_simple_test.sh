#!/bin/bash

# EMFAD® Simple Test Build
# Vereinfachter Build für Samsung S21 Ultra Testing

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 EMFAD® Simple Test Build${NC}"
echo -e "${BLUE}===========================${NC}"

# Check if we're in the right directory
if [ ! -f "build_simple.gradle" ]; then
    echo -e "${RED}❌ build_simple.gradle not found${NC}"
    exit 1
fi

# Backup original build.gradle
if [ -f "build.gradle" ]; then
    echo -e "${YELLOW}📦 Backing up original build.gradle${NC}"
    cp build.gradle build.gradle.backup
fi

# Use simple build.gradle
echo -e "${BLUE}🔄 Using simplified build configuration${NC}"
cp build_simple.gradle build.gradle

# Create gradle wrapper if it doesn't exist
if [ ! -f "gradlew" ]; then
    echo -e "${BLUE}📦 Creating Gradle Wrapper${NC}"
    
    # Create gradle wrapper files
    mkdir -p gradle/wrapper
    
    cat > gradle/wrapper/gradle-wrapper.properties << EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.0-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

    cat > gradlew << 'EOF'
#!/bin/sh

# Gradle start up script for UN*X

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
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
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=Gradle\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
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
    echo -e "${GREEN}✅ Gradle Wrapper created${NC}"
fi

# Clean and build
echo -e "${BLUE}🧹 Cleaning project${NC}"
./gradlew clean

echo -e "${BLUE}🔨 Building debug APK${NC}"
./gradlew assembleDebug

# Check if build was successful
if [ -f "build/outputs/apk/debug/com.emfad.app-debug.apk" ]; then
    APK_SIZE=$(du -h "build/outputs/apk/debug/com.emfad.app-debug.apk" | cut -f1)
    echo -e "${GREEN}✅ Build successful!${NC}"
    echo -e "📱 APK: ${GREEN}com.emfad.app-debug.apk${NC} (${APK_SIZE})"
    
    # Install on Samsung S21 Ultra if connected
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
    if [ -f "$ADB" ] && $ADB devices | grep -q "device$"; then
        echo -e "${BLUE}📲 Installing on Samsung S21 Ultra${NC}"
        $ADB install -r "build/outputs/apk/debug/com.emfad.app-debug.apk"
        
        echo -e "${BLUE}🚀 Starting app${NC}"
        $ADB shell am start -n com.emfad.app.debug/.MainActivity
        
        echo -e "${GREEN}✅ App installed and started!${NC}"
        echo -e "${CYAN}📱 Check your Samsung S21 Ultra device${NC}"
    else
        echo -e "${YELLOW}⚠️  Samsung S21 Ultra not connected${NC}"
        echo -e "Connect device and run: adb install -r build/outputs/apk/debug/com.emfad.app-debug.apk"
    fi
else
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

# Restore original build.gradle
if [ -f "build.gradle.backup" ]; then
    echo -e "${BLUE}🔄 Restoring original build.gradle${NC}"
    mv build.gradle.backup build.gradle
fi

echo -e "${GREEN}🎉 Simple test build completed!${NC}"
