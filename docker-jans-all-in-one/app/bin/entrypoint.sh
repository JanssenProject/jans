#!/usr/bin/env bash

set -e

get_prometheus_lib() {
    if [ -n "${CN_PROMETHEUS_PORT}" ]; then
        prom_agent_version="0.17.2"

        if [ ! -f /opt/prometheus/jmx_prometheus_javaagent.jar ]; then
            wget -q https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${prom_agent_version}/jmx_prometheus_javaagent-${prom_agent_version}.jar -O /opt/prometheus/jmx_prometheus_javaagent.jar
        fi
    fi
}

show_ps_output() {
    while true
    do
        ps aux | more
        sleep 30
    done
}

get_prometheus_lib
python3 /app/jans_aio/bootstrap.py

if [ "$CN_AIO_ENABLE_MONITOR" = "true" ]; then
    show_ps_output &
fi

exec supervisord -c /app/conf/supervisord.conf
