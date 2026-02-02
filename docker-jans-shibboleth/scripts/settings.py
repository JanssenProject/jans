#!/usr/bin/env python3

import os

LOGGING_CONFIG = {
    "version": 1,
    "formatters": {
        "default": {
            "format": "%(levelname)s - %(name)s - %(message)s",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "default",
        },
    },
    "loggers": {
        "shibboleth": {
            "handlers": ["console"],
            "level": os.environ.get("CN_SHIBBOLETH_LOG_LEVEL", "INFO"),
            "propagate": False,
        },
        "jans.pycloudlib": {
            "handlers": ["console"],
            "level": os.environ.get("CN_PYCLOUDLIB_LOG_LEVEL", "INFO"),
            "propagate": False,
        },
    },
    "root": {
        "level": "INFO",
        "handlers": ["console"],
    },
}
