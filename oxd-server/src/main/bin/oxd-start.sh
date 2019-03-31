#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=$(dirname $0)
CONF=$BASEDIR/../conf/oxd-server.yml
echo BASEDIR=$BASEDIR
echo CONF=$CONF

LIB=$BASEDIR/../lib
javaExe=java
$javaExe -Djava.net.preferIPv4Stack=true -cp %LIB%/bcprov-jdk15on-1.54.jar:$LIB/oxd-server.jar org.gluu.oxd.server.OxdServerApplication server $CONF
