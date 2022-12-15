#!/bin/bash
set -e

# ================================================================================================ #
# Check if this is a  user loading job to  the backend couchbase                                   #
# ================================================================================================ #

if [[ "${LOAD_USERS_TO_COUCHBASE}" == "true" ]]; then
  /usr/bin/python3 /scripts/add_users_couchbase.py
  exit 0
fi

# ================================================================================================ #
# Check if this is a  user loading job to  the backend ldap                                        #
# ================================================================================================ #

if [[ "${LOAD_USERS_TO_LDAP}" == "true" ]]; then
  /usr/bin/python3 /scripts/add_users_ldap.py
  exit 0
fi

# ================================================================================================ #
# Check if this is a  user loading job to  the backend spanner                                     #
# ================================================================================================ #

if [[ "${LOAD_USERS_TO_SPANNER}" == "true" ]]; then
  /usr/bin/python3 /scripts/add_users_spanner.py
  exit 0
fi

# ================================================================================================ #
# Check if this is a  user loading job to  the backend spanner                                     #
# ================================================================================================ #

if [[ "${LOAD_USERS_TO_SPANNER}" == "true" ]]; then
  /usr/bin/python3 /scripts/add_users_spanner.py
  exit 0
fi


replace_all() {
  IFS='.' read -ra FQDN_PARTS <<< "$FQDN"
  sed "s#FQDN#$FQDN#g" \
    | sed "s#AUTHZ_CLIENT_ID#$AUTHZ_CLIENT_ID#g" \
    | sed "s#ROPC_CLIENT_ID#$ROPC_CLIENT_ID#g" \
    | sed "s#AUTHZ_CLIENT_SECRET#$AUTHZ_CLIENT_SECRET#g" \
    | sed "s#ROPC_CLIENT_SECRET#$ROPC_CLIENT_SECRET#g" \
    | sed "s#FQDN_PART1#${FQDN_PARTS[1]}#g" \
    | sed "s#FQDN_PART2#${FQDN_PARTS[2]}#g" \
    | sed "s#FQDN_PART3#${FQDN_PARTS[3]}#g" \
    | sed "s#FIRST_BATCH_MIN#$FIRST_BATCH_MIN#g" \
    | sed "s#FIRST_BATCH_MAX#$FIRST_BATCH_MAX#g" \
    | sed "s#SECOND_BATCH_MIN#$SECOND_BATCH_MIN#g" \
    | sed "s#SECOND_BATCH_MAX#$SECOND_BATCH_MAX#g" \
    | sed "s#TEST_USERS_PREFIX_STRING#$TEST_USERS_PREFIX_STRING#g"

}

cat /scripts/tests/authorization_code_flow.jmx | replace_all > tmpfile && mv tmpfile /scripts/authorization_code_flow.jmx
cat /scripts/tests/ropc.jmx | replace_all > tmpfile && mv tmpfile /scripts/ropc.jmx

if [ "$RUN_AUTHZ_TEST" == "true" ]
then
  echo "Authentication code flow is activated."
  # Add -o modules.console.disable=true to disable TUI
  bzt /scripts/tests/authorization_code_flow.jmx
  exit 0
fi

if [ "$RUN_ROPC_TEST" == "true" ]
then
  echo "Resource owner password credential grant flow is activated."
  # Add -o modules.console.disable=true to disable TUI
  bzt /scripts/tests/ropc.jmx
  exit 0
fi

