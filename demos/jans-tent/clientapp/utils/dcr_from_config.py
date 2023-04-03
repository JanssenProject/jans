import logging
from clientapp import config as cfg

OP_URL = cfg.ISSUER
REDIRECT_URIS = cfg.REDIRECT_URIS

def setup_logging() -> None:
    logging.getLogger('oic')
    logging.getLogger('urllib3')
    logging.basicConfig(
        level=logging.DEBUG,
        handlers=[logging.StreamHandler(), logging.FileHandler('register_new_client.log')],
        format='[%(asctime)s] %(levelname)s %(name)s in %(module)s : %(message)s')

