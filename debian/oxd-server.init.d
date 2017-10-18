### BEGIN INIT INFO
# Provides:          oxd-server
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start daemon at boot time
# Description:       Enable service provided by daemon.
### END INIT INFO

SERVICE_NAME=oxd-server
PID_PATH_NAME=/var/run/oxd-server.pid
BASEDIR=/opt/oxd-server/bin/
CONF=/opt/oxd-server/conf
LIB=/opt/oxd-server/lib

do_start () {        
        if [ ! -f $PID_PATH_NAME ]; then                
                echo "Starting $SERVICE_NAME ..."                
                nohup java -Doxd.server.config=$CONF/oxd-conf.json -Dlog4j.configuration=$CONF/log4j.xml -cp $LIB/bcprov-jdk15on-1.54.jar:$LIB/resteasy-jaxrs-2.3.7.Final.jar:$LIB/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.ServerLauncher 2>>/dev/null>>/dev/null&                
                echo $! > $PID_PATH_NAME        
        else                
                echo "$SERVICE_NAME is already running ..."        
        fi        
        PID_NUM=$(cat $PID_PATH_NAME)        
        echo "PID: [$PID_NUM]"
}

do_stop () {        
        if [ -f $PID_PATH_NAME ]; then            
                PID=$(cat $PID_PATH_NAME);            
                echo "$SERVICE_NAME stoping ..."            
                kill $PID;            
                rm $PID_PATH_NAME        
        else            
                echo "$SERVICE_NAME is not running ..."        
        fi
}

case $1 in
    start)
            do_start
    ;;
    stop)
            do_stop
    ;;
    restart)
            do_stop
            do_start
    ;;
    status)
        if [ -f $PID_PATH_NAME ]; then
            echo "$SERVICE_NAME is running ...";
            PID_NUM=$(cat $PID_PATH_NAME)                
            echo "PID: [$PID_NUM]"
        else
           echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    *)
        echo "Usage: $0 {start|stop|status|restart}"
        RETVAL=2
    ;;
esac 
