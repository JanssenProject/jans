#!/bin/bash
set -euo pipefail
MAIN_DIRECTORY_LOCATION=$1
OUTPUT_DIRECTORY=$2
RELEASE_TAG=${3:-nightly}
SETTINGS="$MAIN_DIRECTORY_LOCATION"/.github/maven-settings.xml
if [ "$RELEASE_TAG" = "nightly" ]; then
    UNIFFI_VERSION="0.0.0"
else
    UNIFFI_VERSION="${RELEASE_TAG#v}"
fi
RELEASE_URL="https://github.com/JanssenProject/jans/releases/download/$RELEASE_TAG"

JVM_PROJECTS="agama jans-auth-server jans-casa jans-config-api jans-core jans-cedarling/bindings/cedarling-java jans-fido2 jans-keycloak-link jans-link jans-lock jans-orm jans-scim"
for module in $JVM_PROJECTS
 do
   echo "Generating javadocs for module: $module and all it's sub-modules"
   if [ "$module" = "jans-cedarling/bindings/cedarling-java" ]; then
    BASE_DIR="$MAIN_DIRECTORY_LOCATION/jans-cedarling/bindings/cedarling-java"
    RES_DIR="${BASE_DIR}/src/main/resources"
    KOTLIN_DIR="${BASE_DIR}/src/main/kotlin/io/jans/cedarling"
    mkdir -p "$RES_DIR" "$KOTLIN_DIR"
    wget -q "$RELEASE_URL/libcedarling_uniffi-${UNIFFI_VERSION}.so" -O "${RES_DIR}/libcedarling_uniffi.so"
    ZIP_PATH="${KOTLIN_DIR}/cedarling_uniffi-kotlin-${UNIFFI_VERSION}.zip"
    wget -q "$RELEASE_URL/cedarling_uniffi-kotlin-${UNIFFI_VERSION}.zip" -O "$ZIP_PATH"
    unzip -q "$ZIP_PATH" -d "$KOTLIN_DIR"
    rm -f "$ZIP_PATH"
    mvn -q -s "$SETTINGS" -f "$MAIN_DIRECTORY_LOCATION"/"$module"/pom.xml dokka:javadoc
    echo "getting locations where javadocs got generated"
    doc_path_pattern="*/target/dokkaJavadoc"
    doc_subpath="target/dokkaJavadoc"
   else
    mvn -q -s "$SETTINGS" -f "$MAIN_DIRECTORY_LOCATION"/"$module"/pom.xml javadoc:javadoc
    doc_path_pattern="*/target/site/apidocs"
    doc_subpath="target/site/apidocs"
   fi

   echo "getting locations where javadocs got generated"
   generated_doc_paths=()
   while IFS= read -r generated_doc_path; do
     generated_doc_paths+=("$generated_doc_path")
   done < <(find "$MAIN_DIRECTORY_LOCATION/$module" -type d -path "$doc_path_pattern" | sed "s|/$doc_subpath||")
   if [ ${#generated_doc_paths[@]} -eq 0 ]; then
     echo "ERROR: no javadocs were generated for module '$module'." >&2
     exit 1
   fi

   echo "move javadocs from each location to respective documentation site location"
   for source_path in "${generated_doc_paths[@]}"
   do
     target_path="$OUTPUT_DIRECTORY/${source_path#"$MAIN_DIRECTORY_LOCATION"/}"
     echo "Copying javadocs from $source_path to $target_path"
     mkdir -p "$target_path"
     cp -r "$source_path/$doc_subpath/"* "$target_path/"
   done
 done
