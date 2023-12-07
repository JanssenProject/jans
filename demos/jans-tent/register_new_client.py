# executes a new client auto-register from config.py
import logging
from clientapp.utils.dcr_from_config import register

# add independent logging for CLI script
logging.getLogger('oic')
logging.getLogger('urllib3')
logging.basicConfig(
    level=logging.DEBUG,
    handlers=[logging.StreamHandler(), logging.FileHandler('register_new_client.log')],
    format='[%(asctime)s] %(levelname)s %(name)s in %(module)s : %(message)s')
register()
