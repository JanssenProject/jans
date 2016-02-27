### BEGIN INIT INFO
# Provides:          gluu-oxd-server
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start daemon at boot time
# Description:       Enable service provided by daemon.
### END INIT INFO

SERVICE_NAME=gluu-oxd-server
PID_PATH_NAME=/var/run/gluu-oxd-server.pid
BASEDIR=/opt/oxd-server/bin/
CONF=/opt/oxd-server/conf
LIB=/opt/oxd-server/lib

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -Doxd.server.config=$CONF/oxd-conf.json -Dlog4j.configuration=$CONF/log4j.xml -cp $LIB/bcprov-jdk16-1.46.jar:$LIB/resteasy-jaxrs-2.3.4.Final.jar:$LIB/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.ServerLauncher 2>>/dev/null>>/dev/null&
                        echo $! > $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            nohup java -Doxd.server.config=$CONF/oxd-conf.json -Dlog4j.configuration=$CONF/log4j.xml -cp $LIB/bcprov-jdk16-1.46.jar:$LIB/resteasy-jaxrs-2.3.4.Final.jar:$LIB/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.ServerLauncher 2>>/dev/null>>/dev/null&
                        echo $! > $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    status)
        if [ -f $PID_PATH_NAME ]; then
            echo "$SERVICE_NAME is running ...";
        else
           echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    *)
        echo "Usage: $0 {start|stop|status|restart}"
        RETVAL=2
    ;;
esac 
