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
        "jans-fido2": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False,
        },
    },
}
