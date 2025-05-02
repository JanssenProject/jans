#!/usr/bin/env bash

set -e

get_prometheus_lib() {
    if [ -n "${CN_PROMETHEUS_PORT}" ]; then
        agent_version=${PROMETHEUS_AGENT_VERSION:-1.0.1}

        if [ ! -f /opt/prometheus/jmx_prometheus_javaagent.jar ]; then
            curl -sS "https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${agent_version}/jmx_prometheus_javaagent-${agent_version}.jar" -o /opt/prometheus/jmx_prometheus_javaagent.jar
        fi
    fi
}

get_prometheus_lib
python3 /app/jans_aio/bootstrap.py

exec supervisord -c /app/conf/supervisord.conf
