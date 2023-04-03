import logging
from logging.handlers import TimedRotatingFileHandler


def setup_logger() -> None:
    FORMATTER = logging.Formatter("[%(asctime)s] %(levelname)s %(name)s in %(module)s : %(message)s")
    LOG_FILE = "test-client.log"
    file_handler = TimedRotatingFileHandler(LOG_FILE, when='midnight')
    file_handler.setFormatter(FORMATTER)
    logging.basicConfig(level=logging.DEBUG, handlers=[file_handler])
