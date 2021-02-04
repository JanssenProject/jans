import os
import re
import json

dbname = 'jans'
sql_type = 'mysql'

cur_dir = os.path.dirname(os.path.realpath(__file__))

with open(os.path.join(cur_dir, 'sql_index.json')) as f:
    sql_indexes = json.load(f)

w = open(os.path.join(cur_dir, 'jans_index.sql'), 'w')

for table in sql_indexes[sql_type]:
    for field in sql_indexes[sql_type][table]['fields']:
        w.write('ALTER TABLE {0}.{1} ADD INDEX `{1}_{2}` (`{3}`);\n'.format(
            dbname,
            table,
            re.sub(r'[^0-9a-zA-Z\s]+','_', field),
            field
            ))
    for i, custom in enumerate(sql_indexes[sql_type][table]['custom']):
        w.write('ALTER TABLE {0}.{1} ADD INDEX `{1}_{2}` (`{3}`);\n'.format(
            dbname,
            table,
            i,
            custom
            ))
w.close()
    
