import logging
from logging.handlers import TimedRotatingFileHandler


def setup_logger() -> None:
    formatter = logging.Formatter("[%(asctime)s] %(levelname)s %(name)s in %(module)s : %(message)s")
    log_file = "test-client.log"
    file_handler = TimedRotatingFileHandler(log_file, when='midnight')
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    file_handler.setFormatter(formatter)
    logging.getLogger("oic")
    logging.getLogger("oauth")
    logging.getLogger("flask-oidc")
    logging.getLogger("urllib3")
    logging.basicConfig(level=logging.DEBUG, handlers=[file_handler, console_handler])
