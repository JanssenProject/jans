import logging
import urllib.parse

from clientapp import config as cfg
from clientapp.helpers.client_handler import ClientHandler
import json
from urllib import parse

OP_URL = cfg.ISSUER
REDIRECT_URIS = cfg.REDIRECT_URIS
SCOPE = cfg.SCOPE
parsed_redirect_uri = urllib.parse.urlparse(cfg.REDIRECT_URIS[0])
POST_LOGOUT_REDIRECT_URI = '%s://%s' % (parsed_redirect_uri.scheme, parsed_redirect_uri.netloc)


def setup_logging() -> None:
    logging.getLogger('oic')
    logging.getLogger('urllib3')
    logging.basicConfig(
        level=logging.DEBUG,
        handlers=[logging.StreamHandler(), logging.FileHandler('register_new_client.log')],
        format='[%(asctime)s] %(levelname)s %(name)s in %(module)s : %(message)s')


def register() -> None:
    """
    Register client with information from config and write info to client_info.json
    :return: None
    """
    logger = logging.getLogger(__name__)
    scope_as_list = SCOPE.split(" ")
    additional_params = {
        'scope': scope_as_list,
        'post_logout_redirect_uris': [POST_LOGOUT_REDIRECT_URI]
    }
    client_handler = ClientHandler(OP_URL, REDIRECT_URIS, additional_params)
    json_client_info = json.dumps(client_handler.get_client_dict(), indent=4)
    with open('client_info.json', 'w') as outfile:
        logger.info('Writing registered client information to client_info.json')
        outfile.write(json_client_info)

