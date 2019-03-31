#!/usr/bin/env bash
# Startup script for oxd-server under *nix systems (it works under NT/cygwin too).
JAVA_HOME=/opt/jre
JAVA=$JAVA_HOME/bin/java
OXD_LOGS=/var/log/oxd-server
OXD_HOME=/opt/oxd-server
CONF=/opt/oxd-server/conf
LIB=$OXD_HOME/lib
JAVA_OPTIONS="-server -Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:+DisableExplicitGC -Djava.net.preferIPv4Stack=true -cp /opt/oxd-server/lib/bcprov-jdk15on-1.54.jar:/opt/oxd-server/lib/oxd-server.jar org.gluu.oxd.server.OxdServerApplication server /opt/oxd-server/conf/oxd-server.yml"
OXD_USER=jetty
##################################################
# Set the name which is used by other variables.
# Defaults to the file name without extension.
##################################################
NAME=$(echo $(basename $0) | sed -e 's/^[SK][0-9]*//' -e 's/\.sh$//')

# To get the service to restart correctly on reboot, uncomment below (3 lines):
# ========================
# chkconfig: 3 99 99
# description: oxd server
# processname: oxd-server
# ========================

# Configuration files
#
# /etc/default/$NAME
#   If it exists, this is read at the start of script. It may perform any
#   sequence of shell commands, like setting relevant environment variables.
#
# $HOME/.$NAMErc (e.g. $HOME/.oxd-serverrc)
#   If it exists, this is read at the start of script. It may perform any
#   sequence of shell commands, like setting relevant environment variables.
#
#   The files will be checked for existence before being passed to oxd-server.
#
# Configuration variables
#
# JAVA
#   Command to invoke Java. If not set, java (from the PATH) will be used.
#
# JAVA_OPTIONS
#   Extra options to pass to the JVM
#
# OXD_HOME
#   Where oxd is installed. If not set, the script will try go
#   guess it by looking at the invocation path for the script
#
# OXD_BASE
#   Where your oxd server base directory is.  If not set, the value from
#   $OXD_HOME will be used.
#
# OXD_RUN
#   Where the $NAME.pid file should be stored. It defaults to the
#   first available of /var/run, /usr/var/run OXD_BASE and /tmp
#   if not set.
#
# OXD_PID_FILE
#   The oxd-server PID file, defaults to $OXD_RUN/$NAME.pid
#
# OXD_ARGS
#   The default arguments to pass to oxd-server.
#
# OXD_USER
#   if set, then used as a username to run the server as
#
# OXD_SHELL
#   If set, then used as the shell by su when starting the server.  Will have
#   no effect if start-stop-daemon exists.  Useful when OXD_USER does not
#   have shell access, e.g. /bin/false
#
# OXD_INIT_LOG
SERVICE_NAME="oxd-server"
OXD_INIT_LOG=/var/log/oxd-server/oxd-server.log

usage()
{
    echo "Usage: ${0##*/} [-d] {start|stop|run|restart|check|supervise} [ CONFIGS ... ] "
    exit 1
}

[ $# -gt 0 ] || usage


##################################################
# Some utility functions
##################################################
findDirectory()
{
  local L OP=$1
  shift
  for L in "$@"; do
    [ "$OP" "$L" ] || continue
    printf %s "$L"
    break
  done
}

running()
{
  if [ -f "$1" ]
  then
    local PID=$(cat "$1" 2>/dev/null) || return 1
    kill -0 "$PID" 2>/dev/null
    return
  fi
  rm -f "$1"
  return 1
}

started()
{
  # wait for 60s to see "STARTED" in PID file
  for T in 1 2 3 4 5 6 7 9 10 11 12 13 14 15
  do
    sleep 4
    [ -z "$(grep STARTED $1 2>/dev/null)" ] || return 0
    [ -z "$(grep STOPPED $1 2>/dev/null)" ] || return 1
    [ -z "$(grep FAILED $1 2>/dev/null)" ] || return 1
    local PID=$(cat "$2" 2>/dev/null) || return 1
    kill -0 "$PID" 2>/dev/null || return 1
    echo -n ". "
  done

  return 1;
}


readConfig()
{
  (( DEBUG )) && echo "Reading $1.."
  source "$1"
}



##################################################
# Get the action & configs
##################################################
CONFIGS=()
NO_START=0
DEBUG=0

while [[ $1 = -* ]]; do
  case $1 in
    -d) DEBUG=1 ;;
  esac
  shift
done
ACTION=$1
shift

##################################################
# Read any configuration files
##################################################
ETC=/etc
if [ $UID != 0 ]
then
  ETC=$HOME/etc
fi

for CONFIG in {/etc,~/etc}/default/${NAME}{,9} $HOME/.${NAME}rc; do
  if [ -f "$CONFIG" ] ; then
    readConfig "$CONFIG"
  fi
done


##################################################
# Set tmp if not already set.
##################################################
TMPDIR=${TMPDIR:-/tmp}

##################################################
# oxd-server's hallmark
##################################################
OXD_INSTALL_TRACE_FILE="oxd-server.jar"


##################################################
# Try to determine OXD_HOME if not set
##################################################
if [ -z "$OXD_HOME" ]
then
  OXD_SH=$0
  case "$OXD_SH" in
    /*)     OXD_HOME=${OXD_SH%/*/*} ;;
    ./*/*)  OXD_HOME=${OXD_SH%/*/*} ;;
    ./*)    OXD_HOME=.. ;;
    */*/*)  OXD_HOME=./${OXD_SH%/*/*} ;;
    */*)    OXD_HOME=. ;;
    *)      OXD_HOME=.. ;;
  esac

  if [ ! -f "$OXD_HOME/lib/$OXD_INSTALL_TRACE_FILE" ]
  then
    OXD_HOME=
  fi
fi


##################################################
# No OXD_HOME yet? We're out of luck!
##################################################
if [ -z "$OXD_HOME" ]; then
  echo "** ERROR: OXD_HOME not set, you need to set it or install in a standard location"
  exit 1
fi

cd "$OXD_HOME"
OXD_HOME=$PWD


##################################################
# Set OXD_BASE
##################################################
if [ -z "$OXD_BASE" ]; then
  OXD_BASE=$OXD_HOME
fi

cd "$OXD_BASE"
OXD_BASE=$PWD


#####################################################
# Check that oxd server is where we think it is
#####################################################
if [ ! -r "$OXD_HOME/lib/$OXD_INSTALL_TRACE_FILE" ]
then
  echo "** ERROR: Oops! oxd server doesn't appear to be installed in $OXD_HOME"
  echo "** ERROR:  $OXD_HOME/lib/$OXD_INSTALL_TRACE_FILE is not readable!"
  exit 1
fi

#####################################################
# Find a location for the pid file
#####################################################
if [ -z "$OXD_RUN" ]
then
  OXD_RUN=$(findDirectory -w /var/run /usr/var/run $OXD_BASE /tmp)
fi

#####################################################
# Find a pid and state file
#####################################################
if [ -z "$OXD_PID_FILE" ]
then
  OXD_PID_FILE="$OXD_RUN/${NAME}.pid"
fi

get_pid() {
        if [ -f $OXD_PID_FILE ]; then
                PID_NUM=$(cat $OXD_PID_FILE)
                echo "$PID_NUM"
        else
                PID_NUM="`ps -eaf|grep -i java|grep -v grep|grep -i 'oxd-conf.json'|awk '{print $2}'`"
                ###For one more possible bug, find and kill oxd
                if [ "x$PID_NUM" != "x" ]; then
                        echo "$PID_NUM"
                fi
        fi
}

if [ -z "$OXD_STATE" ]
then
  OXD_STATE=$OXD_BASE/${NAME}.state
fi

case "`uname`" in
CYGWIN*) OXD_STATE="`cygpath -w $OXD_STATE`";;
esac


OXD_ARGS=(${OXD_ARGS[*]} "oxd-server.state=$OXD_STATE")

##################################################
# Setup JAVA if unset
##################################################
if [ -z "$JAVA" ]
then
  JAVA=$(which java)
fi

if [ ! -f "$JAVA" ]
then
  JAVA=$(which java)
fi

if [ -z "$JAVA" ]
then
  echo "Cannot find a Java JDK. Please either set JAVA or put java (>=1.8) in your PATH." >&2
  exit 1
fi

if [[ "$JAVA" ]]; then
    version=$("$JAVA" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ "$version" < "1.8" ]]; then
        echo "JAVA version is $version. Please set java (>=1.8)." >&2
        exit 1
    fi
fi

#####################################################
# See if OXD_LOGS is defined
#####################################################
if [ -z "$OXD_LOGS" ] && [ -d $OXD_BASE/logs ]
then
  OXD_LOGS=$OXD_BASE/logs
fi
if [ -z "$OXD_LOGS" ] && [ -d $OXD_HOME/logs ]
then
  OXD_LOGS=$OXD_HOME/logs
fi
if [ "$OXD_LOGS" ]
then

  case "`uname`" in
  CYGWIN*) OXD_LOGS="`cygpath -w $OXD_LOGS`";;
  esac

  #JAVA_OPTIONS=(${JAVA_OPTIONS[*]} "-Doxd.logging.dir=$OXD_LOGS")
fi

#####################################################
# Are we running on Windows? Could be, with Cygwin/NT.
#####################################################
case "`uname`" in
CYGWIN*) PATH_SEPARATOR=";";;
*) PATH_SEPARATOR=":";;
esac


#####################################################
# Add oxd server properties to Java VM options.
#####################################################

case "`uname`" in
CYGWIN*)
OXD_HOME="`cygpath -w $OXD_HOME`"
OXD_BASE="`cygpath -w $OXD_BASE`"
TMPDIR="`cygpath -w $TMPDIR`"
;;
esac

#JAVA_OPTIONS=(${JAVA_OPTIONS[*]} "-Doxd.home=$OXD_HOME" "-Doxd.base=$OXD_BASE" "-Djava.io.tmpdir=$TMPDIR")
#####################################################
# This is how the oxd server will be started
#####################################################

OXD_START="org.gluu.oxd.server.ServerLauncher"

case "`uname`" in
CYGWIN*) OXD_START="`cygpath -w $OXD_START`";;
esac

RUN_ARGS=(${JAVA_OPTIONS[@]} "$OXD_START" ${OXD_ARGS[*]})
RUN_CMD=("$JAVA" ${RUN_ARGS[@]})

#####################################################
# Comment these out after you're happy with what
# the script is doing.
#####################################################
if (( DEBUG ))
then
  echo "OXD_HOME     =  $OXD_HOME"
  echo "OXD_BASE     =  $OXD_BASE"
  echo "OXD_CONF     =  $OXD_CONF"
  echo "OXD_PID_FILE      =  $OXD_PID_FILE"
  echo "OXD_START    =  $OXD_START"
  echo "OXD_ARGS     =  ${OXD_ARGS[*]}"
  echo "JAVA_OPTIONS   =  ${JAVA_OPTIONS[*]}"
  echo "JAVA           =  $JAVA"
  echo "RUN_CMD        =  ${RUN_CMD[*]}"
fi
dip_in_logs() {
	if [ ! -f $OXD_INIT_LOG ]; then
		sleep 10
	fi
	echo "Checking logs for possible errors:"
	INIT_START_STATUS=`tail -n 1 $OXD_INIT_LOG`
	while true;
	do
		if [ "x$INIT_START_STATUS" != "x" ]; then
			if [ "x$PREV_START_STATUS" = "x" ]; then
				PREV_START_STATUS=$INIT_START_STATUS
				sleep 10
				INIT_START_STATUS=`tail -n 1 $OXD_INIT_LOG`
			fi
		fi
		if [ "$INIT_START_STATUS" != "$PREV_START_STATUS" ]; then
			PREV_START_STATUS=$INIT_START_STATUS	
			sleep 10
			INIT_START_STATUS=`tail -n 1 $OXD_INIT_LOG`
		else
			break;
		fi
	done	
}

do_start () {
        PID_NUM=`get_pid`
        if [ "x$PID_NUM" = "x" ]; then
                echo "Starting $SERVICE_NAME:"

    		if [ $UID -eq 0 ] && type start-stop-daemon > /dev/null 2>&1
    		then
      			unset CH_USER
      			if [ -n "$OXD_USER" ]
      			then
        			CH_USER="-c$OXD_USER"
      			fi
	
			start-stop-daemon --start --quiet --chuid $OXD_USER --make-pidfile --pidfile $OXD_PID_FILE --background --exec $JAVA -- ${JAVA_OPTIONS[@]} >> $OXD_LOGS/start.log 2>&1

			#dip_in_logs
			sleep 4
			for i in `seq 1 24`;
                        do
                	        START_STATUS=`tail -n 4 $OXD_INIT_LOG|grep -i 'o.e.j.s.Server -  Started'` > /dev/null 2>&1
                	        ERROR_STATUS=`tail -n 10 $OXD_INIT_LOG|egrep -i "Failed to start oxd server|Error"` > /dev/null 2>&1			        
			        if [ "x$START_STATUS" != "x" ] || [ "x$ERROR_STATUS" != "x" ]; then
		         	        break
		         	fi
				sleep 5
                        done
                	if [ "x$START_STATUS" = "x" ]; then
                        	###If by chance log file doesn't provide necessary string, sleep another 10 seconds and check again PID of process
                        	if [ "x$ERROR_STATUS" != "x" ]; then
                                	### Since error occurred, we should remove the PID file at this point itself.
					kill -9 `cat $OXD_PID_FILE` > /dev/null 2>&1
					rm -f  $OXD_PID_FILE
                                	echo "Some errors encountered..."
                                	echo "See log below: "
                                	echo ""
                                	echo "$ERROR_STATUS"
                                	echo ""
                                	echo "For details please check $OXD_INIT_LOG ."
                                	echo "Exiting..."
                                	exit 1
				else
                                	### Since error occurred, we should remove the PID file at this point itself.
					kill -9 `cat $OXD_PID_FILE` > /dev/null 2>&1
					rm -f  $OXD_PID_FILE
                                	echo "Some errors encountered..."
                                	echo ""
                                	echo "Exiting..."
                                	exit 1				   
                        	fi
	
                	fi
        		chown "$OXD_USER" "$OXD_PID_FILE" > /dev/null 2>&1
		else
      			if [ -n "$OXD_USER" ] && [ `whoami` != "$OXD_USER" ]
      			then
        			unset SU_SHELL
        			if [ "$OXD_SHELL" ]
        			then
          				SU_SHELL="-s $OXD_SHELL"
        			fi

        			touch "$OXD_PID_FILE"
        			chown "$OXD_USER" "$OXD_PID_FILE"
        			# FIXME: Broken solution: wordsplitting, pathname expansion, arbitrary command execution, etc.
        			su - "$OXD_USER" $SU_SHELL -c "
				exec $JAVA ${JAVA_OPTIONS[@]} >> "$OXD_LOGS/start.log" 2>&1 &
          			disown \$!
          			echo \$! > '$OXD_PID_FILE'"
				#dip_in_logs
			        sleep 4
			        for i in `seq 1 24`;
                                do
                		        START_STATUS=`tail -n 4 $OXD_INIT_LOG|grep -i 'o.e.j.s.Server -  Started'` > /dev/null 2>&1
                		        ERROR_STATUS=`tail -n 10 $OXD_INIT_LOG|egrep -i "Failed to start oxd server|Error"` > /dev/null 2>&1                	                		        
			                if [ "x$START_STATUS" != "x" ] || [ "x$ERROR_STATUS" != "x" ]; then
		         	                break
		                 	fi
				        sleep 5
                                done					
                		if [ "x$START_STATUS" = "x" ]; then
	                        	###If by chance log file doesn't provide necessary string, sleep another 10 seconds and check again PID of process
	                        	if [ "x$ERROR_STATUS" != "x" ]; then
	                                	### Since error occurred, we should remove the PID file at this point itself.
						kill -9 `cat $OXD_PID_FILE` > /dev/null 2>&1
						rm -f  $OXD_PID_FILE
	                                	echo "Some errors encountered..."
	                                	echo "See log below: "
	                                	echo ""
	                                	echo "$ERROR_STATUS"
	                                	echo ""
	                                	echo "For details please check $OXD_INIT_LOG ."
	                                	echo "Exiting..."
	                                	exit 1
					else
	                                	### Since error occurred, we should remove the PID file at this point itself.
						kill -9 `cat $OXD_PID_FILE` > /dev/null 2>&1
						rm -f  $OXD_PID_FILE
	                                	echo "Some errors encountered..."
	                                	echo ""
	                                	echo "Exiting..."
	                                	exit 1				   
	                        	fi	
	                	fi
      			else
        			$JAVA ${JAVA_OPTIONS[@]} > /dev/null &
        			disown $!
        			echo $! > "$OXD_PID_FILE"
      			fi
               	fi 	
        	echo "PID: [`get_pid`]"
        	echo "OK `date`"
       	else
                if netstat -tulpn | grep "$PID_NUM/java"; then
                        echo "$SERVICE_NAME is already running ..."
                        echo "PID: [$PID_NUM]"
                        exit 1
                else
                        kill -9 `cat $OXD_PID_FILE` > /dev/null 2>&1
                        rm -f  $OXD_PID_FILE
                        do_start
                        exit 0
                fi
	fi
}

do_stop () {
	PID_NUM=`get_pid`
        if [ "x$PID_NUM" != "x" ]; then 
    		echo -n "Stopping $SERVICE_NAME: "
    		if [ $UID -eq 0 ] && type start-stop-daemon > /dev/null 2>&1; then
      			start-stop-daemon -K -p"$OXD_PID_FILE" -d"$OXD_HOME" -a "$JAVA" -s HUP

      			TIMEOUT=30
      			while running "$OXD_PID_FILE"; do
        			if (( TIMEOUT-- == 0 )); then
          				start-stop-daemon -K -p"$OXD_PID_FILE" -d"$OXD_HOME" -a "$JAVA" -s KILL
        			fi

        			sleep 1
      			done
    		else
      			if [ ! -f "$OXD_PID_FILE" ] ; then
        			echo "ERROR: no pid found at $OXD_PID_FILE"
        			exit 1
      			fi

      			PID=$(cat "$OXD_PID_FILE" 2>/dev/null)
      			if [ -z "$PID" ] ; then
        			echo "ERROR: no pid id found in $OXD_PID_FILE"
        			exit 1
      			fi
      			kill "$PID" 2>/dev/null

      			TIMEOUT=30
      			while running $OXD_PID_FILE; do
        			if (( TIMEOUT-- == 0 )); then
          				kill -KILL "$PID" 2>/dev/null
        			fi

        			sleep 1
      			done
    		fi

    		rm -f "$OXD_PID_FILE"
    		rm -f "$OXD_STATE"
    		echo OK
	else
                echo "$SERVICE_NAME is not running ..."     
		exit 1
	fi
}
##################################################
# Do the action
##################################################
case "$ACTION" in
  start)
	do_start
    ;;

  stop)
	do_stop
    ;;

  restart)
    OXD_SH=$0
    > "$OXD_STATE"

    "$OXD_SH" stop "$@"
    "$OXD_SH" start "$@"

    ;;

  supervise)
    #
    # Under control of daemontools supervise monitor which
    # handles restarts and shutdowns via the svc program.
    #
    exec "${RUN_CMD[@]}"

    ;;

  run|demo)
    echo "Running oxd server: "

    if running "$OXD_PID_FILE"
    then
      echo Already Running $(cat "$OXD_PID_FILE")!
      exit 1
    fi

    exec "${RUN_CMD[@]}"
    ;;

  check|status)
    if running "$OXD_PID_FILE"
    then
      echo "oxd server running pid=$(< "$OXD_PID_FILE")"
    else
      echo "oxd server NOT running"
    fi
    echo
    echo "OXD_HOME     =  $OXD_HOME"
    echo "OXD_BASE     =  $OXD_BASE"
    echo "OXD_CONF     =  $OXD_CONF"
    echo "OXD_PID_FILE      =  $OXD_PID_FILE"
    echo "OXD_START    =  $OXD_START"
    echo "OXD_LOGS     =  $OXD_LOGS"
    echo "OXD_STATE    =  $OXD_STATE"
    echo "CLASSPATH      =  $CLASSPATH"
    echo "JAVA           =  $JAVA"
    echo "JAVA_OPTIONS   =  ${JAVA_OPTIONS[*]}"
    echo "OXD_ARGS     =  ${OXD_ARGS[*]}"
    echo "RUN_CMD        =  ${RUN_CMD[*]}"
    echo

    if running "$OXD_PID_FILE"
    then
      exit 0
    fi
    exit 1

    ;;

  *)
    usage

    ;;
esac

exit 0
