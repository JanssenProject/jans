#!/bin/bash
set -euo pipefail
# The below variable represents the top level directory of the repository
MAIN_DIRECTORY_LOCATION=$1
# GitHub Packages auth (server id "github"); needs GITHUB_ACTOR + JANS_TOKEN in the env.
SETTINGS="$MAIN_DIRECTORY_LOCATION"/.github/maven-settings.xml
echo "Generate Swagger yaml SPEC"

# Compile jans-config-api to generate new Swagger SPECs from API annotations
mvn -q -s "$SETTINGS" -f "$MAIN_DIRECTORY_LOCATION"/jans-config-api/pom.xml -DskipTests clean compile