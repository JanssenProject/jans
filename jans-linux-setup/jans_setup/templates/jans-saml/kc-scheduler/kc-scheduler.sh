#! /bin/bash

JAVA_HOME=%(jre_home)s
SCHEDULER_HOME=%(scheduler_dir)s
SCHEDULER_VERSION=v%(jans_version)s

APP_CONFIG_FILE=${SCHEDULER_HOME}/conf/config.properties
LOG_CONFIG_FILE=${SCHEDULER_HOME}/conf/logback.xml

if [ -z "$JAVA" ]; then
    if [ -n "$JAVA_HOME" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

$JAVA -Dapp.config="${APP_CONFIG_FILE}" \
  -Dlogback.configurationFile="${SCHEDULER_HOME}/conf/logback.xml" \
  -Dapp.version=${SCHEDULER_VERSION} -Dapp.home="${SCHEDULER_HOME}" \
  -cp "${SCHEDULER_HOME}/lib/*" io.jans.kc.scheduler.App
