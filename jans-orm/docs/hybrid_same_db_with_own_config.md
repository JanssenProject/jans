

## This guide explains how to install 2 Gluu CE servers which uses same MySQL DB expect `ou=configuration`

Both servers can work with same or different salt. Same salt will allow to use same OpenID clients on both servers.

### First server deployment with main DB.
1. Create VM which conform minimal CE requirements.
2. Open 3306 port to allow access MySQL DB on this server.
3. Install CE into it.
4. In MySQL server file `/etc/mysql/mysql.conf.d/mysqld.cnf` configure to listen on 
```
bind-address		= 0.0.0.0
```

5. Restart MySQL service.
```
systemctl restart mysql
```
6. Configure `gluu` DB `user`to allow access from another server:
 - Create `/root/.mysql` with admin MySql user password and protect file.
 - Run `mysql -u root --password="$(cat /root/.mysql)" -e "CREATE USER 'gluu'@'%' IDENTIFIED BY '$(cat /root/.mysql)'"`
 - Run `mysql -u root --password="$(cat /root/.mysql)" -e "GRANT ALL PRIVILEGES ON gluudb.* TO 'gluu'@'%'"`

  Note: if you need to provide access from specifc IP replace `'gluu'@'%'` in both commands with command with IP address `'gluu'@'%XX.XX.XX.XX'`

7. It's not required step. This server is also possible to configure to use Hybrid ORM if in deployment you will use 3 DB: 2 DB with `ou=configuration` and third with all shared tables.
 - Create Hybrid ORM configuration files.
```
cp /etc/gluu/conf/gluu-sql.properties /etc/gluu/conf/gluu-sql.shared.properties
chown root:gluu /etc/gluu/conf/gluu-sql.shared.properties
# Update gluu-sql.shared.properties if needed to specify connection details to third DB

cat > /etc/gluu/conf/gluu-hybrid.properties
# Configure 2 storages which will use configs gluu-sql.properties and gluu-sql.shared.properties
storages: sql, sql.shared
# Default storage is sql
storage.default: sql
# Specify which base RDN to store in sql.shared storage
storage.sql.shared.mapping:
# Specify which base RDN to store in sql storage
storage.sql.mapping:
```
 - Replace in `/etc/gluu/conf/gluu.properties` line `persistence.type=sql` with `persistence.type=hybrid`
8. Check if there are no errors in oxAuth/Identity/etc... logs. These services should automatically reload ORM configuration on `/etc/gluu/conf/gluu.properties` update.
9. Remove all manually created temporary password files for security reasons.


### Second server deployment.
1. Create VM which conform minimal CE requirements.
2. This step is not mandatory but needed if you want to use same OpenID clients on both servers.
 - Copy `/install/community-edition-setup/setup.properties` from first server into `/root/setup.properties`. This file is encrypted after install. You need to decrypt it with command which setup provided at the end of install:
```
cd /install/community-edition-setup
openssl enc -d -aes-256-cbc -in setup.properties.last.enc -out setup.properties
```
 - Update `hostname`, `oxd_server_https` and `ip` property in `/root/setup.properties` and update other properties if needed.
3. Install CE into this VM. To run setup with preconfigured salt use command: `./setup.py -f /root/setup.properties`
4. Create Hybrid ORM configuration files.
 - Run commands.
```
cp /etc/gluu/conf/gluu-sql.properties /etc/gluu/conf/gluu-sql.shared.properties
chown root:gluu /etc/gluu/conf/gluu-sql.shared.properties

cat > /etc/gluu/conf/gluu-hybrid.properties
# Configure 2 storages which will use configs gluu-sql.properties and gluu-sql.shared.properties
storages: sql, sql.shared
# Default storage is sql.shared
storage.default: sql.shared
# Specify which base RDN to store in sql.shared storage
storage.sql.shared.mapping:
# Specify which base RDN to store in sql storage
# In current configuration ou=configuration will be stored in sql storage
storage.sql.mapping: configuration
```
 - In /etc/gluu/conf/gluu-sql.shared.properties update
```
connection.uri=jdbc:mysql://localhost:3306/gluudb?enabledTLSProtocols=TLSv1.2
```
We need to specify first server IP/DNS instead of localhost
 - Replace in `/etc/gluu/conf/gluu.properties` line `persistence.type=sql` with `persistence.type=hybrid`
5. Check if there are no errors in oxAuth/Identity/etc... logs. These services should autiomatically reload ORM configuration on `/etc/gluu/conf/gluu.properties` update.
6. Log into Identity on first server, find oxTrust OpenId client and update `Redirect Login URIs` and `Post Logout Redirect URIs`. We need to provide there URLs with second DNS name as well.
7. Remove all manually created temporary password files for security reasons.
8. Check if you can log into Identity from both DNS addresses.
