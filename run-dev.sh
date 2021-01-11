#!/bin/bash
#
# /etc/init.d/run-callboard.sh
#
# Startup script for run-callboard
#
# chkconfig: 2345 80 20
# description: Starts and stops sys101_gui_controller
# pidfile: /var/run/om.mibs.asterisk.web.pid

### BEGIN INIT INFO
# Provides:          sys101_gui_controller
# Required-Start:    
# Required-Stop:     
# Should-Start:      
# Should-Stop:       
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: distributed storage system for structured data
# Description:       om.mibs.asterisk.web is a service
### END INIT INFO


APP="web-callboard-1"
DIR=$(/bin/pwd)
CONF=$DIR"/src/main/resources/application.yml"
JAR=$DIR"/build/libs/"$APP".jar"

PID="/var/run/"$APP".pid"

LOG="/var/log/"$APP".log"

# If JAVA_HOME has not been set, try to determine it.
if [ -z "$JAVA_HOME" ]; then
    # If java is in PATH, use a JAVA_HOME that corresponds to that. This is
    # both consistent with how the upstream startup script works, and with
    # the use of alternatives to set a system JVM (as is done on Debian and
    # Red Hat derivatives).
    java="`/usr/bin/which java 2>/dev/null`"
    if [ -n "$java" ]; then
        java=`readlink --canonicalize "$java"`
        JAVA_HOME=`dirname "\`dirname \$java\`"`
    else
        # No JAVA_HOME set and no java found in PATH; search for a JVM.
        for jdir in $JVM_SEARCH_DIRS; do
            if [ -x "$jdir/bin/java" ]; then
                JAVA_HOME="$jdir"
                break
            fi
        done
        # if JAVA_HOME is still empty here, punt.
    fi
fi
JAVA="$JAVA_HOME/bin/java"
export JAVA_HOME JAVA

$JAVA -jar $JAR --spring.config.location=$CONF




   
