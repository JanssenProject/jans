#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=$(dirname $0)
CONF=$BASEDIR/../conf/client-api-server.yml
echo BASEDIR=$BASEDIR
echo CONF=$CONF

LIB=$BASEDIR/../lib
javaExe=java
$javaExe -Djava.net.preferIPv4Stack=true -cp $LIB/jans-client-api-server.jar:$LIB/* io.jans.ca.server.RpServerApplication server $CONF
