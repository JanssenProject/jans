#!/bin/bash
set -euo pipefail
# The below variable represents the top level directory of the repository
MAIN_DIRECTORY_LOCATION=$1
# GitHub Packages auth (server id "github"); needs GITHUB_ACTOR + JANS_TOKEN in the env.
SETTINGS="$MAIN_DIRECTORY_LOCATION"/.github/maven-settings.xml
echo "Generate Swagger yaml SPEC"

# Compile jans-config-api to generate new Swagger SPECs from API annotations.
# Exclude server-fips: it emits no swagger and its build-configapi-fips-war antrun
# (bound to process-sources) needs a packaged war that 'compile' never produces.
mvn -q -s "$SETTINGS" -f "$MAIN_DIRECTORY_LOCATION"/jans-config-api/pom.xml -pl '!server-fips' -DskipTests clean compile