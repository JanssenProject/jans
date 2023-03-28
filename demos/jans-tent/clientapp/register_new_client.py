import config as cfg
from client_handler import ClientHandler
import logging
import json

logging.getLogger('oic')
logging.getLogger('urllib3')
logging.basicConfig(
    level=logging.DEBUG,
    handlers=[logging.StreamHandler(), logging.FileHandler('register_new_client.log')],
    format='[%(asctime)s] %(levelname)s %(name)s in %(module)s : %(message)s')
OP_URL = cfg.ISSUER
CLIENT_URL = 'https://localhost:9090'
logger = logging.getLogger(__name__)
client = ClientHandler(OP_URL, CLIENT_URL)

logger.info('Setting up registered client info')
logger.info(client.get_client_dict())

json_object = json.dumps(client.get_client_dict(), indent=4)

with open('client_info.json', 'w') as outfile:
    outfile.write(json_object)