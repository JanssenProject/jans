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
# CN_ADMIN_PASS: MYSQL and ADMIN user password
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
  echo "install_config_api=""$([[ ${CN_INSTALL_CONFIG_API} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_scim_server=""$([[ ${CN_INSTALL_SCIM} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "installFido2=""$([[ ${CN_INSTALL_FIDO2} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_casa=""$([[ ${CN_INSTALL_CASA} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_jans_lock=""$([[ ${CN_INSTALL_LOCK} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_jans_saml=""$([[ ${CN_INSTALL_SAML} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_jans_keycloak_link=""$([[ ${CN_INSTALL_KC_LINK} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "install_jans_link=""$([[ ${CN_INSTALL_LINK} == true ]] && echo True || echo False)" | tee -a setup.properties > /dev/null
  echo "test_client_id=${TEST_CLIENT_ID}"| tee -a setup.properties > /dev/null
  echo "test_client_pw=${TEST_CLIENT_SECRET}" | tee -a setup.properties > /dev/null1
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

register_fqdn() {
    if [[ "${IS_FQDN_REGISTERED}" == "true" ]]; then
      certbot --apache -d "${CN_HOSTNAME}" -n --agree-tos --email "${CN_EMAIL}" || echo "FQDN was not registered with cerbot"
    fi
}

prepare_auth_server_tests() {
    WORKING_DIRECTORY=$PWD
    echo "*****   cloning jans auth server folder!!   *****"
    rm -rf /tmp/jans || echo "Jans isn't cloned yet..Cloning"\
    && git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout "${JANS_SOURCE_VERSION}" \
    && git sparse-checkout set jans-auth-server jans-scim jans-config-api jans-orm jans-fido2 \
    && cd jans-auth-server \
    && echo "Copying auth server test profiles from ephemeral server" \
    && cp -R /opt/jans/jans-setup/output/test/jans-auth ./ \
    && echo "Creating auth server profile folders" \
    && mkdir -p ./client/profiles/"${CN_HOSTNAME}" \
    && mkdir -p ./server/profiles/"${CN_HOSTNAME}" \
    && echo "Copying auth server profile files" \
    && cp ./jans-auth/client/* ./client/profiles/"${CN_HOSTNAME}" \
    && cp ./jans-auth/server/* ./server/profiles"/${CN_HOSTNAME}" \
    && echo "Copying auth server keystores from default profile" \
    && cp -f ./client/profiles/default/client_keystore.p12 ./client/profiles/"${CN_HOSTNAME}" \
    && cp -f ./server/profiles/default/client_keystore.p12 ./server/profiles/"${CN_HOSTNAME}" \
    && echo "Removing test profile folder" \
    && rm -rf ./jans-auth \
    && cd agama \
    && cp /opt/jans/jans-setup/output/test/agama/config-agama-test.properties . \
    && mkdir -p ./engine/profiles/"${CN_HOSTNAME}" \
    && mv config-agama-test.properties ./engine/profiles/"${CN_HOSTNAME}"/config-agama-test.properties  \
    && cd .. \
    && echo "Checking if the compilation and install is ok without running the tests" \
    && echo "Installing the jans cert in local keystore" \
    && openssl s_client -connect "${CN_HOSTNAME}":443 2>&1 |sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/httpd.crt \
    && TrustStorePW=$(grep -Po '(?<=defaultTrustStorePW=)\S+' /opt/jans/jans-setup/setup.properties.last) \
    && keytool -import -trustcacerts -noprompt -storepass "${TrustStorePW}" -alias "${CN_HOSTNAME}" -keystore /usr/lib/jvm/java-11-openjdk-amd64/lib/security/cacerts -file /tmp/httpd.crt \
    && cd "$WORKING_DIRECTORY"
}

prepare_scim_test() {
    WORKING_DIRECTORY=$PWD
    cd /tmp/jans \
    && cd jans-scim \
    && echo "Copying scim server test profiles from ephemeral server" \
    && cp -R /opt/jans/jans-setup/output/test/scim-client ./ \
    && echo "Creating scim server profile folders" \
    && mkdir -p ./client/profiles/"${CN_HOSTNAME}" \
    && echo "Copying scim server profile files" \
    && cp ./scim-client/client/config-scim-test.properties ./client/profiles/"${CN_HOSTNAME}" \
    && echo "Removing test profile folder" \
    && rm -rf ./scim-client \
    && cd .. \
    && cd "$WORKING_DIRECTORY"
}

prepare_config_api_test() {
    WORKING_DIRECTORY=$PWD
    cd /tmp/jans \
    && cd jans-config-api \
    && echo "Copying config-api test profiles from ephemeral server" \
    && cp -R /opt/jans/jans-setup/output/test/jans-config-api ./ \
    && rm -rf ./profiles/"${CN_HOSTNAME}" \
    && echo "Creating config-api profile folders" \
    && mkdir -p ./profiles/"${CN_HOSTNAME}" \
    && echo "Copying config-api server profile files" \
    && cp ./jans-config-api/client/* ./profiles/"${CN_HOSTNAME}" \
    && echo "Copying default configuration properties" \
    && cp ./profiles/default/config-build.properties ./profiles/"${CN_HOSTNAME}" \
    && echo "Removing test profile folder" \
    && rm -rf ./jans-config-api \
    && cd .. \
    && cd "$WORKING_DIRECTORY"
}


prepare_java_tests() {
  if [[ "${RUN_TESTS}" == "true" ]]; then
    echo "*****   Running Java tests!!   *****"
    echo "*****   Running Auth server tests!!   *****"
    prepare_auth_server_tests
    echo "*****   Running Scim tests!!   *****"
    prepare_scim_test
    echo "*****   Running Config Api tests!!   *****"
    prepare_config_api_test
    echo "*****   Java tests completed!!   *****"
  fi
}

start_services() {
  /etc/init.d/apache2 start
  /opt/jans/scripts/jans-auth start
  /opt/jans/scripts/jans-config-api start
  /opt/jans/scripts/jans-scim start
  /opt/jans/scripts/jans-fido2 start
  /opt/jans/scripts/jans-casa start ||:  # no-op if script is missing
  /opt/jans/scripts/jans-keycloak-link start ||:  # no-op if script is missing
  /opt/jans/scripts/jans-link start ||:  # no-op if script is missing
}

check_installed_jans
start_services
register_fqdn
prepare_java_tests || "Java test preparations failed!!"

# use -F option to follow (and retry) logs
tail -F /opt/jans/jetty/jans-auth/logs/*.log \
  /opt/jans/jetty/jans-config-api/logs/*.log \
  /opt/jans/jetty/jans-fido2/logs/*.log \
  /opt/jans/jetty/jans-scim/logs/*.log
