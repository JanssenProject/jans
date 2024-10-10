import logging.config
import os

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for
from jans.pycloudlib.validators import validate_persistence_type
from jans.pycloudlib.validators import validate_persistence_hybrid_mapping
from jans.pycloudlib.validators import validate_persistence_sql_dialect

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)


def validate_doc_store_type(value):
    supported_types = ("DB", "LOCAL")

    if value not in supported_types:
        raise ValueError(f"Unsupported document store type {value!r}; please choose one of {','.join(supported_types)}")


def main():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")
    validate_persistence_type(persistence_type)

    if persistence_type == "sql":
        sql_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        validate_persistence_sql_dialect(sql_dialect)

    if persistence_type == "hybrid":
        validate_persistence_hybrid_mapping()

    doc_store_type = os.environ.get("CN_DOCUMENT_STORE_TYPE", "DB")
    validate_doc_store_type(doc_store_type)

    manager = get_manager()
    deps = ["config", "secret"]
    wait_for(manager, deps)


if __name__ == "__main__":
    main()
