"""
Copyright (c) 2025, Gluu, Inc. 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

from datetime import datetime
import logging
import orjson

from structlog import configure, get_logger, stdlib, processors, BytesLoggerFactory, make_filtering_bound_logger, \
    contextvars
from pythonjsonlogger import jsonlogger

configure(
    cache_logger_on_first_use=True,
    wrapper_class=make_filtering_bound_logger(logging.INFO),
    processors=[
        contextvars.merge_contextvars,
        processors.add_log_level,
        processors.format_exc_info,
        processors.TimeStamper(fmt="iso", utc=True),
        processors.JSONRenderer(serializer=orjson.dumps),
    ],
    logger_factory=BytesLoggerFactory(),
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
