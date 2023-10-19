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
        "jans_aio": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": True,
        },
    },
}


# _JANS_AIO_COMPONENTS = (
#     # "configurator",
#     # "persistence_loader",
#     # "jans_auth",
#     # "jans_config_api",
#     # "jans_fido2",
#     # "jans_scim",
#     # "jans-casa",
# )

# for comp in _JANS_AIO_COMPONENTS:
#     LOGGING_CONFIG["loggers"][comp] = {
#         "handlers": ["console"],
#         "level": "INFO",
#         "propagate": False,
#     }
