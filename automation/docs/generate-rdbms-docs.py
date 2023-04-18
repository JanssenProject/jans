import sys
import pymysql


def connectToMySQL(serverIP, serverUser, serverUserPwd, database, characterSet):
    mySQLConnection = pymysql.connect(host=serverIP,
                                      user=serverUser,
                                      password=serverUserPwd,
                                      database=database,
                                      charset=characterSet)
    cursorObject = mySQLConnection.cursor()
    return cursorObject


def getTables(cursorObject, database):
    cursorObject.execute("SHOW tables FROM {}".format(database))
    tables = cursorObject.fetchall()
    return tables  # returns a list of tuples   [('table1',), ('table2',), ('table3',)]


def getTableFields(cursorObject, database, table):
    cursorObject.execute("SHOW COLUMNS FROM {} FROM {}".format(table, database))
    fields = cursorObject.fetchall()
    return fields  # returns a list of tuples   [('field1',), ('field2',), ('field3',)]


def getIndexes(cursorObject, database, table):
    cursorObject.execute("SHOW INDEX FROM {} FROM {}".format(table, database))
    indexes = cursorObject.fetchall()
    return indexes  # returns a list of tuples   [('index1',), ('index2',), ('index3',)]


def printTables(fileName, cursorObject, database, tables):
    with open(fileName, 'w') as f:
        for table in tables:
            f.write("\n\n**" + table[0] + "**\n")
            f.write("|Field|Type|Null|Key|Default|Extra|\n")
            f.write("|-|-|-|-|-|-|")  # Markdown table header row separator
            fields = getTableFields(cursorObject, database, table[0])  # get the fields for the table
            for field in fields:
                f.write("\n|" + "|".join(map(str, field)))


def printIndexes(fileName, cursorObject, database, tables):
    with open(fileName, 'w') as f:
        for table in tables:
            f.write("\n\n**" + table[0] + "**\n")
            indexes = getIndexes(cursorObject, database, table[0])
            f.write(
                "|Table|Non_unique|Key_name|Seq_in_index|Column_name|Collation|Cardinality|Sub_part|Packed|Null|Index_type"
                "|Comment| Index_comment | Visible | Expression|\n")
            f.write("|-|-|-|-|-|-|-|-|-|-|-|-| - | - | -|")
            for index in indexes:
                f.write("\n|" + "|".join(map(str, index)))

def main():
    serverIP = sys.argv[1] or "localhost"
    serverUser = sys.argv[2] or "jans"
    serverUserPwd = sys.argv[3] or "abcde"
    database = sys.argv[4] or "jansdb"
    characterSet = sys.argv[5] or "utf8"
    schemaFileName = sys.argv[6] or "mysql-schema.md"
    schemaIndexesFileName = sys.argv[7] or "mysql-schema-indexes.md"

    cursorObject = connectToMySQL(serverIP, serverUser, serverUserPwd, database, characterSet)
    tables = getTables(cursorObject, database)
    printTables(schemaFileName, cursorObject, database, tables)
    printIndexes(schemaIndexesFileName, cursorObject, database, tables)


if __name__ == '__main__':
    main()
