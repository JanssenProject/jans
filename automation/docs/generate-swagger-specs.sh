#!/bin/bash
set -euo pipefail

echo "Generate Swagger yaml SPEC"

# Compile jans-config-api to generate new Swagger SPECs from API annotations
mvn -q -f jans-config-api/pom.xml -DskipTests clean compile