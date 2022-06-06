LOGGING_CONFIG = {
    "version": 1,
    "formatters": {
        "default": {
            "format": "%(levelname)s - %(name)s - %(asctime)s - %(message)s",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "default",
        },
    },
    "loggers": {
        "jans.pycloudlib": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": True,
        },
        "entrypoint": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
        "ldap_setup": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
        "sql_setup": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
        "spanner_setup": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
        "couchbase_setup": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },

    },
    # "root": {
    #     "level": "INFO",
    #     "handlers": ["console"],
    # },
}
