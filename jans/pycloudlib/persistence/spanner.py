import os

from google.api_core.exceptions import AlreadyExists
from google.api_core.exceptions import FailedPrecondition
from google.cloud import spanner


class SpannerClient:
    def __init__(self):
        # The following envvars are required:
        #
        # - ``GOOGLE_APPLICATION_CREDENTIALS`` json file that should be injected in upstream images
        # - ``GCLOUD_PROJECT`` (a.k.a Google project ID)
        client = spanner.Client()
        instance_id = os.environ.get("CN_SPANNER_INSTANCE_ID", "")
        self.instance = client.instance(instance_id)

        database_id = os.environ.get("CN_SPANNER_DATABASE_ID", "")
        self.database = self.instance.database(database_id)

    def connected(self):
        cntr = 0
        with self.database.snapshot() as snapshot:
            result = snapshot.execute_sql("SELECT 1")
            for item in result:
                cntr = item[0]
                break
            return cntr > 0

    def create_table(self, table_name: str, column_mapping: dict, pk_column: str):
        columns = []
        for column_name, column_type in column_mapping.items():
            column_def = f"{self.quoted_id(column_name)} {column_type}"

            if column_name == pk_column:
                column_def += " NOT NULL"
            columns.append(column_def)

        columns_fmt = ", ".join(columns)
        pk_def = f"PRIMARY KEY ({self.quoted_id(pk_column)})"
        query = f"CREATE TABLE {self.quoted_id(table_name)} ({columns_fmt}) {pk_def}"

        try:
            self.database.update_ddl([query])
        except FailedPrecondition as exc:
            if "Duplicate name in schema" in exc.args[0]:
                # table exists
                pass
            else:
                raise

    def quoted_id(self, identifier):
        char = '`'
        return f"{char}{identifier}{char}"

    def get_table_mapping(self) -> dict:
        type_code = (
            "TYPE_CODE_UNSPECIFIED",
            "BOOL",
            "INT64",
            "FLOAT64",
            "TIMESTAMP",
            "DATE",
            "STRING",
            "BYTES",
            "ARRAY",
            "STRUCT",
            "NUMERIC",
        )

        def parse_field_type(type_):
            name = type_code[type_.code]
            # if name == "ARRAY":
            #     name = f"{name}<{type_code(type_.array_element_type.code)}>"
            return name

        table_mapping = {}
        for table in self.database.list_tables():
            table_mapping[table.table_id] = {
                field.name: parse_field_type(field.type_)
                for field in table.schema
            }
        return table_mapping

    def insert_into(self, table_name, column_mapping):
        # TODO: handle ARRAY<STRING(MAX)> ?
        def insert_rows(transaction):
            transaction.insert(
                table_name,
                columns=column_mapping.keys(),
                values=[column_mapping.values()]
            )

        try:
            self.database.run_in_transaction(insert_rows)
        except AlreadyExists:
            pass

    def row_exists(self, table_name, id_):
        exists = False
        with self.database.snapshot() as snapshot:
            result = snapshot.read(
                table=table_name,
                columns=["doc_id"],
                keyset=spanner.KeySet([
                    [id_]
                ])
            )
            for _ in result:
                exists = True
                break
        return exists

    def create_index(self, query):
        try:
            self.database.update_ddl([query])
        except FailedPrecondition as exc:
            if "Duplicate name in schema" in exc.args[0]:
                # table exists
                pass
            else:
                raise


def render_spanner_properties(manager, src: str, dest: str) -> None:
    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        cred_file = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", "")
        creds = f"auth.credentials-file={cred_file}"
        rendered_txt = txt % {
            "spanner_project": os.environ.get("GCLOUD_PROJECT", ""),
            "spanner_instance": os.environ.get("CN_SPANNER_INSTANCE_ID", ""),
            "spanner_database": os.environ.get("CN_SPANNER_DATABASE_ID", ""),
            "spanner_creds": creds,
        }
        f.write(rendered_txt)
