SERVICE_NAME=gluu-oxd-server
PID_PATH_NAME=/var/run/gluu-oxd-server.pid
BASEDIR=/opt/oxd-server/bin/
CONF=/opt/oxd-server/conf/oxd-conf.json
LIB=/opt/oxd-server/lib

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -Doxd.server.config=$CONF -cp $LIB/bcprov-jdk16-1.46.jar:$LIB/resteasy-jaxrs-2.3.4.Final.jar:$LIB/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.ServerLauncher 2>> /dev/null >> /dev/null & 
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
            nohup java -Doxd.server.config=$CONF -cp $LIB/bcprov-jdk16-1.46.jar:$LIB/resteasy-jaxrs-2.3.4.Final.jar:$LIB/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.ServerLauncher 2>> /dev/null >> /dev/null & 
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
