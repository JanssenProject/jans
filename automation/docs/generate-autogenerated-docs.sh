#!/bin/bash
set -euo pipefail

echo "Generate properties and feature flag documents from elements annotated with @DocFeatureFlag and @DocProperty"

# Compile jans-core to pick-up any changes in annotation processors
mvn -q -f jans-core/pom.xml -DskipTests clean compile install

# Compile modules where classes that use these annotations exist.
# This will generate markdown files under target/classes directory
mvn -q -f jans-auth-server/pom.xml clean compile
mvn -q -f jans-fido2/pom.xml clean compile
mvn -q -f jans-scim/pom.xml clean compile

# Move markdown files to appropriate locations under documentation root 'doc'
mv -f jans-auth-server/model/target/classes/janssenauthserver-properties.md docs/admin/reference/json/properties
mv -f jans-auth-server/model/target/classes/janssenauthserver-feature-flags.md docs/admin/reference/json/feature-flags
mv -f jans-fido2/model/target/classes/fido2-properties.md docs/admin/reference/json/properties
mv -f jans-scim/model/target/classes/scim-properties.md docs/admin/reference/json/properties
