#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=/opt/client-api-server
CONF=/opt/client-api-server/conf/client-api-server.yml
LIB=/opt/client-api-server/lib

echo BASEDIR=$BASEDIR
echo CONF=$CONF

javaExe=java
$javaExe -cp $LIB/jans-client-api-server.jar:$LIB/* Cli -c $CONF "$@"
