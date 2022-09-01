#!/usr/bin/env bash
set -e

# ======================================================================================================================
# INSTALL JANSSEN
# PASSED VARS:
# JANS_SOURCE_VERSION: Specifies the exact commit version to build off of
# HOSTNAME : hostname i.e test.jans.io
# ORG_NAME : Organization name i.e Janssen
# EMAIL: i.e support@jans.io
# CITY: i.e Austin
# STATE: i.e TX
# COUNTRY: i.e US
# ADMIN_PASS: LDAP and ADMIN user password
# INSTALL_LDAP
# INSTALL_CONFIG_API
# INSTALL_SCIM_SERVER
# INSTALL_CLIENT_API

# ======================================================================================================================

# Functions

install_gluu() {
  echo "*****   Writing properties!!   *****"
  echo "hostname=${HOSTNAME}" | tee -a setup.properties > /dev/null
  # shellcheck disable=SC2016
  echo "admin_password=${ADMIN_PASS}" | tee -a setup.properties > /dev/null
  echo "orgName=${ORG_NAME}" | tee -a setup.properties > /dev/null
  echo "admin_email=${EMAIL}" | tee -a setup.properties > /dev/null
  echo "city=${CITY}" | tee -a setup.properties > /dev/null
  echo "state=${STATE}" | tee -a setup.properties > /dev/null
  echo "countryCode=${COUNTRY}" | tee -a setup.properties > /dev/null
  # shellcheck disable=SC2016
  echo "ldapPass=${ADMIN_PASS}" | tee -a setup.properties > /dev/null
  echo "installLdap=""$([[ ${INSTALL_LDAP} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_config_api=""$([[ ${INSTALL_CONFIG_API} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_scim_server=""$([[ ${INSTALL_SCIM_SERVER} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "installFido2=""$([[ ${INSTALL_FIDO2} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_client_api=""$([[ ${INSTALL_CLIENT_API} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null

  if [[ "${INSTALL_LDAP}" == "false" ]]; then
    echo "rdbm_install=True" | tee -a setup.properties > /dev/null
    echo "rdbm_install_type=1" | tee -a setup.properties > /dev/null
    echo "rdbm_type=mysql" | tee -a setup.properties > /dev/null
    echo "rdbm_host=localhost" | tee -a setup.properties > /dev/null
  fi

  echo "*****   Running the setup script for ${ORG_NAME}!!   *****"
  curl https://raw.githubusercontent.com/JanssenProject/jans/"${JANS_SOURCE_VERSION}"/jans-linux-setup/jans_setup/install.py > install.py
  python3 install.py -yes --args="-f setup.properties -n"
  echo "*****   Setup script completed!!    *****"

}

install_gluu 2>&1 | tee setup_log
