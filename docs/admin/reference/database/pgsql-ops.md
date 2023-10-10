---
tags:
  - administration
  - reference
  - database
---

# PostgreSQL Operations

PostgreSQL is a versatile and reliable database management system that empowers developers and organizations to build robust 
and efficient applications.

## Establish Connection to Jans PostgreSQL Server

Connect to PostgreSql workspace: `sudo -u postgres psql` 

Show all Database list : `\list` or `\l`

You will see `jansdb` in the list of database.

Let's make a connection with `jansdb` : `\c jansdb`


## Search user

* Change database: `\c jansdb`
* Search for user `testUser`: `SELECT * FROM "jansPerson" WHERE uid = 'testuser';`
  * If you want pretty output, enable display mode with `\x` 
  * Re-run search query. 


## Change password for user jans

* Changing user 'jans' password to "secret": `ALTER USER jans WITH PASSWORD 'secret';` 

## Add user in Jans Group

* Get DN of target user. i.e. we are searching for DN of user 'testUser' with: `SELECT * FROM "jansPerson" WHERE uid = 'testuser';`
* Get DN of Jans Admin Group. i.e. `SELECT * FROM "jansGrp";`
* Add ( actually append ) new user in `member` of this group: 
```
UPDATE "jansGrp" SET member = '["inum=d33a2ce9-e9de-4f74-8a7d-2519f73635b7,ou=people,o=jans", "inum=618d7792-caca-4915-8b92-9955bf94affb,ou=people,o=jans"]';
```

## List users with specific filter

To search for users with a filter using PostgreSQL's command-line tool psql, you can use the `SELECT` statement with the `WHERE` clause to apply filters to the query. Here's the query to find specific user:

```
SELECT * FROM "jansPerson" WHERE uid= '<uid>';
```

## Modify column size of Jans postgresql 

The psql command-line tool, you can use the `\d+` command to display detailed information about a table, including its columns. Lets see the details of `jansPerson` table:

```
\d+ "jansPerson";
```

To modify the size of a column in a PostgreSQL table, you will need to use the `ALTER TABLE` statement along with the `ALTER COLUMN` clause. Here's how you can modify the size of a column:

```
ALTER TABLE "jansPerson" 
ALTER COLUMN mail TYPE VARCHAR(100);
```

## Add custom attribute

To add a custom attribute to an existing PostgreSQL table, you can use the `ALTER TABLE` statement with the `ADD COLUMN` clause. Here's how you can add a custom attribute in `jansPerson` table:

```
ALTER TABLE "jansPerson"
ADD COLUMN membership_level VARCHAR(50);
```
You can also specify additional constraints, defaults, or other attributes for the new column as needed. Here's an example with a `default` value:

```
ALTER TABLE "jansPerson"
ADD COLUMN membership_level VARCHAR DEFAULT 'Basic';
```

## Output column data into txt

If you want to output the data from a PostgreSQL table's column into a text file, you can use the `COPY` command. This command allows you to copy the contents of a table or query result into an external file. Here's how you can do it:

```
COPY (SELECT * FROM "jansPerson") TO '/tmp/output.txt';
```

After executing the command, the data from the specified column will be copied into the specified text file.

## Back-up and re-store 

PostgreSQL is a popular open-source relational database management system used for web applications, business intelligence, and other data-intensive applications. A critical aspect of managing a PostgreSQL database is ensuring data protection by having a backup and restore strategy in place.

### Back-up
To `dump` the PostgreSQL database, you can use the `pg_dump` command-line utility.
To back-up your database 

```
pg_dump -h localhost -U "<user>" "<dbName>" -Fc > /tmp/back-up.sql
```

### Re-store

To restore the backup file you need to use `pg_restore`.

```
pg_restore -h localhost -U "<user>" -d <db_name> <back-up.sql>
```




## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
