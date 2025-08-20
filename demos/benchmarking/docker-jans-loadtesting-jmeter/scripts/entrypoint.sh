#!/bin/bash
set -e

# ================================================================================================ #
# Check if this is a  user loading job to  the backend RDBMS                                     #
# ================================================================================================ #
if [[ "$LOAD_USERS_TO_RDBMS" = "true" ]]; then
  /usr/bin/python3 /scripts/add_users_rdbm.py
  exit 0
fi


replace_all() {
  users_range=$(((USER_NUMBER_ENDING_POINT-USER_NUMBER_STARTING_POINT)))
  FIRST_BATCH_MIN="$USER_NUMBER_STARTING_POINT"
  SECOND_BATCH_MAX="$USER_NUMBER_ENDING_POINT"
  FIRST_BATCH_MAX=$((((users_range*10)/100)))
  SECOND_BATCH_MIN=$((FIRST_BATCH_MAX+1))

  IFS='.' read -ra FQDN_PARTS <<< "$FQDN"
  sed "s#AUTHZ_CLIENT_ID#$AUTHZ_CLIENT_ID#g" \
    | sed "s#ROPC_CLIENT_ID#$ROPC_CLIENT_ID#g" \
    | sed "s#AUTHZ_CLIENT_SECRET#$AUTHZ_CLIENT_SECRET#g" \
    | sed "s#ROPC_CLIENT_SECRET#$ROPC_CLIENT_SECRET#g" \
    | sed "s#CLIENT_CREDENTIALS_CLIENT_ID#$CLIENT_CREDENTIALS_CLIENT_ID#g" \
    | sed "s#CLIENT_CREDENTIALS_CLIENT_SECRET#$CLIENT_CREDENTIALS_CLIENT_SECRET#g" \
    | sed "s#DCR_CLIENT_ID#$DCR_CLIENT_ID#g" \
    | sed "s#DCR_CLIENT_SECRET#$DCR_CLIENT_SECRET#g" \
    | sed "s#FQDN_PART1#${FQDN_PARTS[0]}#g" \
    | sed "s#FQDN_PART2#${FQDN_PARTS[1]}#g" \
    | sed "s#FQDN_PART3#${FQDN_PARTS[2]}#g" \
    | sed "s#FQDN#$FQDN#g" \
    | sed "s#FIRST_BATCH_MIN#$FIRST_BATCH_MIN#g" \
    | sed "s#FIRST_BATCH_MAX#$FIRST_BATCH_MAX#g" \
    | sed "s#SECOND_BATCH_MIN#$SECOND_BATCH_MIN#g" \
    | sed "s#SECOND_BATCH_MAX#$SECOND_BATCH_MAX#g" \
    | sed "s#THREAD_COUNT#$THREAD_COUNT#g" \
    | sed "s#TEST_USERS_PREFIX_STRING#$TEST_USERS_PREFIX_STRING#g"

}

cat /scripts/tests/authorization_code_flow.jmx | replace_all > tmpfile && mv tmpfile /scripts/authorization_code_flow.jmx
cat /scripts/tests/authorization_code_flow_soak.jmx | replace_all > tmpfile && mv tmpfile /scripts/authorization_code_flow_soak.jmx
cat /scripts/tests/resource_owner_password_credentials.jmx | replace_all > tmpfile && mv tmpfile /scripts/resource_owner_password_credentials.jmx
cat /scripts/tests/register_client.jmx | replace_all > tmpfile && mv tmpfile /scripts/register_client.jmx
cat /scripts/tests/client_credentials.jmx | replace_all > tmpfile && mv tmpfile /scripts/client_credentials.jmx

if [[ "$RUN_AUTHZ_SOAK_TEST" = "true" ]]
then
  echo "Authentication code flow soak test is activated."
  # Add -o modules.console.disable=true to disable TUI
  bzt /scripts/authorization_code_flow_soak.jmx
  exit 0
fi

if [[ "$RUN_AUTHZ_TEST" = "true" ]]
then
  echo "Authentication code flow is activated."
  # Add -o modules.console.disable=true to disable TUI
  bzt /scripts/authorization_code_flow.jmx
  exit 0
fi

if [[ "$RUN_ROPC_TEST" = "true" ]]
then
  echo "Resource owner password credential grant flow is activated."
  # Add -o modules.console.disable=true to disable TUI
  bzt /scripts/resource_owner_password_credentials.jmx
  exit 0
fi

if [[ "$RUN_DCR_TEST" = "true" ]]
then
  echo "DCR is activated."
  # Add -o modules.console.disable=true to disable TUI
  bzt /scripts/register_client.jmx
  exit 0
fi

if [[ "$RUN_CLIENT_CREDENTIALS_TEST" = "true" ]]
then
  echo "Client Credentials test is activated."
  # Add -o modules.console.disable=true to disable TUI
  bzt /scripts/client_credentials.jmx
  exit 0
fi
