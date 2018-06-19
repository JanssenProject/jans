#!/bin/sh
# Please run it with JDK 1.6 or higher

BASEDIR=$(dirname $0)
CONF=$BASEDIR/../conf/oxd-server.yml
echo BASEDIR=$BASEDIR
echo CONF=$CONF

LIB=$BASEDIR/../lib
javaExe=java
#javaExe=/usr/java/jdk1.6.0_30/bin/java
$javaExe -jar $LIB/oxd-server.jar org.xdi.oxd.server.OxdServerApplication $CONF
