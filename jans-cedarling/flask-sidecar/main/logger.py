from datetime import datetime

from structlog import configure, stdlib, processors, get_logger 
from pythonjsonlogger import jsonlogger

configure(
    processors=[
        stdlib.filter_by_level,
        stdlib.add_logger_name,
        stdlib.add_log_level,
        stdlib.PositionalArgumentsFormatter(),
        processors.TimeStamper(fmt='iso'),
        processors.StackInfoRenderer(),
        processors.format_exc_info,
        processors.JSONRenderer()
    ],
    context_class=dict,
    logger_factory=stdlib.LoggerFactory(),
    wrapper_class=stdlib.BoundLogger,
    cache_logger_on_first_use=True,
)

logger:stdlib.BoundLogger = get_logger()


class JsonLogFormatter(jsonlogger.JsonFormatter):  # pragma: no cover
    def add_fields(self, log_record, record, message_dict):
        """
        This method allows us to inject custom data into resulting log messages
        """
        for field in self._required_fields:
            log_record[field] = record.__dict__.get(field)
        log_record.update(message_dict)

        # Add timestamp and application name if not present
        if "timestamp" not in log_record:
            now = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%S.%fZ")
            log_record["timestamp"] = now

        if "application" not in log_record:
            log_record["application"] = "Flask Sidecar"

        jsonlogger.merge_record_extra(record, log_record, reserved=self._skip_fields)
