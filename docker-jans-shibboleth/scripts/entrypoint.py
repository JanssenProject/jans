#!/usr/bin/env python3

import logging
import os
import sys
import time
from pathlib import Path

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for
from jans.pycloudlib.persistence import render_couchbase_properties
from jans.pycloudlib.persistence import render_sql_properties
from jans.pycloudlib.persistence import render_hybrid_properties

from settings import LOGGING_CONFIG
from shib_setup import ShibbolethSetup

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("shibboleth")

SHIBBOLETH_HOME = os.environ.get("SHIBBOLETH_HOME", "/opt/shibboleth-idp")
JETTY_BASE = os.environ.get("JETTY_BASE", "/opt/shibboleth-idp/jetty")


def main():
    manager = get_manager()
    
    deps = ["config", "secret"]
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")
    
    if persistence_type in ("sql", "hybrid"):
        deps.append("sql")
    if persistence_type in ("couchbase", "hybrid"):
        deps.append("couchbase")

    wait_for(manager, deps)

    logger.info("Starting Shibboleth IDP setup")
    
    setup = ShibbolethSetup(manager)
    setup.configure()
    
    logger.info("Shibboleth IDP setup completed")
    
    start_jetty()


def start_jetty():
    logger.info("Starting Jetty server")
    
    jetty_home = os.environ.get("JETTY_HOME", "/opt/jetty")
    jetty_base = JETTY_BASE
    
    java_opts = os.environ.get("CN_JAVA_OPTIONS", "-Xms256m -Xmx512m")
    
    os.environ["JAVA_OPTIONS"] = java_opts
    os.environ["JETTY_HOME"] = jetty_home
    os.environ["JETTY_BASE"] = jetty_base
    
    os.chdir(jetty_base)
    os.execl(
        "/usr/bin/java",
        "java",
        f"-Didp.home={SHIBBOLETH_HOME}",
        "-Djava.io.tmpdir=/tmp",
        f"-jar", f"{jetty_home}/start.jar",
    )


if __name__ == "__main__":
    main()
