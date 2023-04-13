import logging
from clientapp import config as cfg
from clientapp.helpers.client_handler import ClientHandler
import json

OP_URL = cfg.ISSUER
REDIRECT_URIS = cfg.REDIRECT_URIS


def setup_logging() -> None:
    logging.getLogger('oic')
    logging.getLogger('urllib3')
    logging.basicConfig(
        level=logging.DEBUG,
        handlers=[logging.StreamHandler(), logging.FileHandler('register_new_client.log')],
        format='[%(asctime)s] %(levelname)s %(name)s in %(module)s : %(message)s')


def register() -> None:
    logger = logging.getLogger(__name__)
    client_handler = ClientHandler(OP_URL, REDIRECT_URIS)
    json_client_info = json.dumps(client_handler.get_client_dict(), indent=4)
    with open('client_info.json', 'w') as outfile:
        logger.info('Writing registered client information to client_info.json')
        outfile.write(json_client_info)

