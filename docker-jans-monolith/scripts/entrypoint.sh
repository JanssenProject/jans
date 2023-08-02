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
# CN_INSTALL_LDAP
# CN_INSTALL_CONFIG_API
# CN_INSTALL_SCIM
# RDBMS_DATABASE
# RDBMS_USER
# RDBMS_PASSWORD
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
  echo "installLdap=""$([[ ${CN_INSTALL_LDAP} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_config_api=""$([[ ${CN_INSTALL_CONFIG_API} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_scim_server=""$([[ ${CN_INSTALL_SCIM} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "installFido2=""$([[ ${CN_INSTALL_FIDO2} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "test_client_id=${TEST_CLIENT_ID}"| tee -a setup.properties > /dev/null
  echo "test_client_pw=${TEST_CLIENT_SECRET}" | tee -a setup.properties > /dev/null
  echo "test_client_trusted=""$([[ ${TEST_CLIENT_TRUSTED} == true ]] && echo True || echo True)" | tee -a setup.properties > /dev/null
  echo "loadTestData=True" | tee -a setup.properties > /dev/null
  if [[ "${CN_INSTALL_MYSQL}" == "true" ]] || [[ "${CN_INSTALL_PGSQL}" == "true" ]]; then
    echo "Installing with RDBMS"
    echo "rdbm_install=2" | tee -a setup.properties > /dev/null
    echo "rdbm_install_type=2" | tee -a setup.properties > /dev/null
    echo "rdbm_db=${RDBMS_DATABASE}" | tee -a setup.properties > /dev/null
    echo "rdbm_user=${RDBMS_USER}" | tee -a setup.properties > /dev/null
    echo "rdbm_password=${RDBMS_PASSWORD}" | tee -a setup.properties > /dev/null
    echo "rdbm_host=${RDBMS_HOST}" | tee -a setup.properties > /dev/null
  fi
  if [[ "${CN_INSTALL_MYSQL}" == "true" ]]; then
    echo "Installing with MySql"
    echo "rdbm_type=mysql" | tee -a setup.properties > /dev/null
    echo "rdbm_port=3306" | tee -a setup.properties > /dev/null
  elif [[ "${CN_INSTALL_PGSQL}" == "true" ]]; then
    echo "Installing with Postgres"
    echo "rdbm_type=pgsql" | tee -a setup.properties > /dev/null
    echo "rdbm_port=5432" | tee -a setup.properties > /dev/null
  fi


  echo "*****   Running the setup script for ${CN_ORG_NAME}!!   *****"
  echo "*****   PLEASE NOTE THAT THIS MAY TAKE A WHILE TO FINISH. PLEASE BE PATIENT!!   *****"
  echo "Executing https://raw.githubusercontent.com/JanssenProject/jans/${JANS_SOURCE_VERSION}/jans-linux-setup/jans_setup/install.py > install.py"
  curl https://raw.githubusercontent.com/JanssenProject/jans/"${JANS_SOURCE_VERSION}"/jans-linux-setup/jans_setup/install.py > install.py
  echo "Executing python3 install.py -yes --args=-f setup.properties -n"
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

run_auth_server_test() {
    WORKING_DIRECTORY=$PWD
    echo "*****   cloning jans auth server folder!!   *****"
    git clone --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout "${JANS_SOURCE_VERSION}" \
    && git sparse-checkout set jans-auth-server \
    && cd jans-auth-server \
    && echo "Copying auth server test profiles from ephemeral server" \
    && cp -R /opt/jans/jans-setup/output/test/jans-auth ./ \
    && echo "Creating auth server profile folders" \
    && mkdir -p ./client/profiles/"${CN_HOSTNAME}" \
    && mkdir -p ./server/profiles/"${CN_HOSTNAME}" \
    && echo "Copying auth server profile files" \
    && cp ./jans-auth/client/* ./client/profiles/"${CN_HOSTNAME}" \
    && cp ./jans-auth/server/* ./server/profiles"${CN_HOSTNAME}" \
    && echo "Copying auth server keystores from default profile" \
    && cp -f ./client/profiles/default/client_keystore.p12 ./client/profiles/"${CN_HOSTNAME}" \
    && cp -f ./server/profiles/default/client_keystore.p12 ./server/profiles/"${CN_HOSTNAME}" \
    && echo "Removing test profile folder" \
    && rm -rf ./jans-auth \
    && cd agama \
    && cp /opt/jans/jans-setup/output/test/jans-auth/config-agama-test.properties . \
    && mkdir -p ./engine/profiles/"${CN_HOSTNAME}" \
    && mv config-agama-test.properties ./engine/profiles/"${CN_HOSTNAME}"/config-agama-test.properties  \
    && cd .. \
    && echo "check if the compilation and install is ok without running the tests" \
    && mvn -Dcfg=jans-opensuse.lxd -Dmaven.test.skip=true -fae clean compile install \
    && ehco "install the jans cert in local keystore" \
    && openssl s_client -connect jans-opensuse.lxd:443 2>&1 |sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/httpd.crt \
    && sudo keytool -import -alias "${CN_HOSTNAME}" -keystore /usr/lib64/jvm/java-11-openjdk-11/lib/security/cacerts -file /tmp/httpd.crt \
    && mvn -Dcfg="${CN_HOSTNAME}" -Dmaven.test.skip=false test \
    && cd "$WORKING_DIRECTORY"
}

run_java_tests() {
  if [[ "${RUN_JAVA_TESTS}" == "true" ]]; then
    echo "*****  Installing maven!!   *****"
    apt-get install -y maven
    echo "*****   Running Java tests!!   *****"
    echo "*****   Running Auth server tests!!   *****"
    run_auth_server_test
    echo "*****   Java tests completed!!   *****"
  else
    echo "Maven has not been installed. Enable RUN_JAVA_TESTS to run tests."
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
run_java_tests

# use -F option to follow (and retry) logs
tail -F /opt/jans/jetty/jans-auth/logs/*.log \
  /opt/jans/jetty/jans-config-api/logs/*.log \
  /opt/jans/jetty/jans-fido2/logs/*.log \
  /opt/jans/jetty/jans-scim/logs/*.log
