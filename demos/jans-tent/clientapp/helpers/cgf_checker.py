from os.path import exists
import logging
from clientapp.utils.dcr_from_config import register

logger = logging.getLogger(__name__)


def configuration_exists() -> bool:
    return exists('client_info.json')


def register_client_if_no_client_info() -> None:
    if configuration_exists() :
        logger.info('Found configuration file client_info.json, skipping auto-register')
    else:
        logger.info('Client configuration not found, trying to auto-register through DCR')
        register()
