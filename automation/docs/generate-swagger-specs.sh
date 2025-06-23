#!/bin/bash
set -euo pipefail
# The below variable represents the top level directory of the repository
MAIN_DIRECTORY_LOCATION=$1
echo "Generate Swagger yaml SPEC"

# Compile jans-config-api to generate new Swagger SPECs from API annotations
mvn -q -f "$MAIN_DIRECTORY_LOCATION"/jans-config-api/pom.xml -DskipTests clean compile