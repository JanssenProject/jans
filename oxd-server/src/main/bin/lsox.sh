#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=$(dirname $0)
CONF=/etc/oxd/oxd-server/oxd-conf.json
echo BASEDIR=$BASEDIR
echo CONF=$CONF

LIB=$BASEDIR/../lib
javaExe=java
$javaExe -cp $LIB/oxd-server.jar org.xdi.oxd.server.Cli -c $CONF "$@"
