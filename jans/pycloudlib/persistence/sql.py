import contextlib
import os

from sqlalchemy import create_engine


def get_sql_password() -> str:
    """Get password used for SQL database user.

    :returns: Plaintext password.
    """
    password_file = os.environ.get("CN_SQL_PASSWORD_FILE", "/etc/jans/conf/sql_password")

    password = ""
    with contextlib.suppress(FileNotFoundError):
        with open(password_file) as f:
            password = f.read().strip()
    return password


class SQLClient:
    """This class interacts with SQL database.
    """

    def __init__(self):
        dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        host = os.environ.get("CN_SQL_DB_HOST", "localhost")
        port = os.environ.get("CN_SQL_DB_PORT", 3306)
        database = os.environ.get("CN_SQL_DB_NAME", "jans")
        user = os.environ.get("CN_SQL_DB_USER", "jans")
        password = get_sql_password()

        if dialect == "mysql":
            connector = "mysql+pymysql"

        self.engine = create_engine(
            f"{connector}://{user}:{password}@{host}:{port}/{database}",
            pool_pre_ping=True,
        )

    def is_alive(self):
        """Check whether connection is alive by executing simple query.
        """
        with self.engine.connect() as conn:
            conn.execute("SELECT 1 AS is_alive")
            return True
        return False
