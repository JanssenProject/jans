#!/usr/bin/env bash
set -e

# ======================================================================================================================
# INSTALL JANSSEN
# PASSED VARS:
# JANS_SOURCE_VERSION: Specifies the exact commit version to build off of
# CN_HOSTNAME : hostname i.e test.jans.io
# CN_ORG_NAME : Organization name i.e Janssen
# CN_EMAIL: i.e support@jans.io
# CN_CITY: i.e Austin
# CN_STATE: i.e TX
# CN_COUNTRY: i.e US
# CN_ADMIN_PASS: LDAP or MYSQL and ADMIN user password
# INSTALL_LDAP
# CN_INSTALL_CONFIG_API
# CN_INSTALL_SCIM
# MYSQL_DATABASE
# MYSQL_USER
# MYSQL_PASSWORD
# ======================================================================================================================

IS_JANS_DEPLOYED=/janssen/deployed
# Functions
install_jans() {
  echo "*****   Writing properties!!   *****"
  echo "hostname=${CN_HOSTNAME}" | tee -a setup.properties > /dev/null
  # shellcheck disable=SC2016
  echo "admin_password=${CN_ADMIN_PASS}" | tee -a setup.properties > /dev/null
  echo "orgName=${CN_ORG_NAME}" | tee -a setup.properties > /dev/null
  echo "admin_email=${CN_EMAIL}" | tee -a setup.properties > /dev/null
  echo "city=${CN_CITY}" | tee -a setup.properties > /dev/null
  echo "state=${CN_STATE}" | tee -a setup.properties > /dev/null
  echo "countryCode=${CN_COUNTRY}" | tee -a setup.properties > /dev/null
  # shellcheck disable=SC2016
  echo "ldapPass=${CN_ADMIN_PASS}" | tee -a setup.properties > /dev/null
  echo "installLdap=""$([[ ${INSTALL_LDAP} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_config_api=""$([[ ${CN_INSTALL_CONFIG_API} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_scim_server=""$([[ ${CN_INSTALL_SCIM} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "installFido2=""$([[ ${CN_INSTALL_FIDO2} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null

  if [[ "${INSTALL_LDAP}" == "false" ]]; then
    echo "rdbm_install=2" | tee -a setup.properties > /dev/null
    echo "rdbm_install_type=2" | tee -a setup.properties > /dev/null
    echo "rdbm_db=${MYSQL_DATABASE}" | tee -a setup.properties > /dev/null
    echo "rdbm_user=${MYSQL_USER}" | tee -a setup.properties > /dev/null
    echo "rdbm_password=${MYSQL_PASSWORD}" | tee -a setup.properties > /dev/null
    echo "rdbm_type=mysql" | tee -a setup.properties > /dev/null
    echo "rdbm_host=${MYSQL_HOST}" | tee -a setup.properties > /dev/null
  fi

  echo "*****   Running the setup script for ${CN_ORG_NAME}!!   *****"
  echo "*****   PLEASE NOTE THAT THIS MAY TAKE A WHILE TO FINISH. PLEASE BE PATIENT!!   *****"
  curl https://raw.githubusercontent.com/JanssenProject/jans/"${JANS_SOURCE_VERSION}"/jans-linux-setup/jans_setup/install.py > install.py
  python3 install.py -yes --args="-f setup.properties -n"
  echo "*****   Setup script completed!!    *****"

}

check_installed_jans() {
  if [ -f "$IS_JANS_DEPLOYED" ]; then
    echo "Janssen Authorization has already been installed. Starting services..."
  else
    install_jans 2>&1 | tee setup_log || exit 1
    mkdir janssen
    touch "$IS_JANS_DEPLOYED"
  fi
}

start_services() {
  /etc/init.d/apache2 start
  /opt/dist/scripts/jans-auth start
  /opt/dist/scripts/jans-config-api start
  /opt/dist/scripts/jans-scim start
  /opt/dist/scripts/jans-fido2 start
}

check_installed_jans
start_services

tail -f /opt/jans/jetty/jans-auth/logs/*.log \
-f /opt/jans/jetty/jans-config-api/logs/*.log \
-f /opt/jans/jetty/jans-fido2/logs/*.log \
-f /opt/jans/jetty/jans-scim/logs/*.log
