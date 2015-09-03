#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=$(dirname $0)
CONF=$BASEDIR/../conf/oxd-conf.json
echo BASEDIR=$BASEDIR
echo CONF=$CONF

LIB=$BASEDIR/../lib
javaExe=java
#javaExe=/usr/java/jdk1.6.0_30/bin/java
$javaExe -Doxd.server.config=$CONF -cp $LIB/bcprov-jdk16-1.46.jar:$LIB/resteasy-jaxrs-2.3.4.Final.jar:$LIB/oxd-server-jar-with-dependencies.jar org.xdi.oxd.server.ServerLauncher
