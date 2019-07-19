#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=/opt/oxd-server
CONF=/opt/oxd-server/conf/oxd-server.yml
LIB=/opt/oxd-server/lib

echo BASEDIR=$BASEDIR
echo CONF=$CONF

javaExe=java
$javaExe -cp $LIB/oxd-server.jar org.gluu.oxd.server.Cli -c $CONF "$@"
