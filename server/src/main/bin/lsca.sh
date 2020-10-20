#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=/opt/oxd-server
CONF=/opt/jans-client-api/conf/jans-client-api.yml
LIB=/opt/jans-client-api/lib

echo BASEDIR=$BASEDIR
echo CONF=$CONF

javaExe=java
$javaExe -cp $LIB/jans-client-api-server.jar:$LIB/* Cli -c $CONF "$@"
