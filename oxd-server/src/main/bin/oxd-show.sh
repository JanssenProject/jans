#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=$(dirname $0)
CONF=$BASEDIR/../conf/oxd-conf.json
echo BASEDIR=$BASEDIR
echo CONF=$CONF

LIB=$BASEDIR/../lib
javaExe=java
$javaExe -Doxd.server.config=$CONF -cp $LIB/bcprov-jdk15on-1.54.jar:$LIB/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.Cli "$@"
