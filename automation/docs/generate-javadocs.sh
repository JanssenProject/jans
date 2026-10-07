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

JVM_PROJECTS="agama jans-auth-server jans-casa jans-config-api jans-core jans-cedarling/bindings/cedarling-java jans-fido2 jans-link jans-lock jans-orm jans-scim"
for module in $JVM_PROJECTS
 do
   module_pom="$MAIN_DIRECTORY_LOCATION/$module/pom.xml"
   if [ ! -f "$module_pom" ]; then
     echo "Skipping $module: no pom.xml in this revision"
     continue
   fi
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
    mvn -q -s "$SETTINGS" -f "$module_pom" dokka:javadoc
    doc_subpaths=("target/dokkaJavadoc")
   else
    # FIPS variants depend on their sibling's jar, which javadoc:javadoc never builds.
    fips_excludes=()
    while IFS= read -r submodule; do
      case "$submodule" in
        *fips*) fips_excludes+=("!$submodule") ;;
      esac
    done < <(sed -n 's:.*<module>\(.*\)</module>.*:\1:p' "$module_pom")
    mvn_args=()
    if [ ${#fips_excludes[@]} -gt 0 ]; then
      mvn_args+=(-pl "$(IFS=,; echo "${fips_excludes[*]}")")
      echo "Excluding FIPS modules: ${fips_excludes[*]}"
    fi
    # -fae: submodules depending on a sibling's unpublished jar cannot resolve under a
    # plain javadoc:javadoc, and must not stop the modules that can be documented.
    if ! mvn -q -fae -s "$SETTINGS" -f "$module_pom" ${mvn_args[@]+"${mvn_args[@]}"} javadoc:javadoc; then
      echo "WARNING: some submodules of '$module' failed; publishing the javadocs that were generated."
    fi
    doc_subpaths=("target/reports/apidocs" "target/site/apidocs")
   fi

   echo "move javadocs from each location to respective documentation site location"
   copied=0
   for doc_subpath in "${doc_subpaths[@]}"
   do
     while IFS= read -r generated_doc_path; do
       source_path=${generated_doc_path%/"$doc_subpath"}
       target_path="$OUTPUT_DIRECTORY/${source_path#"$MAIN_DIRECTORY_LOCATION"/}"
       echo "Copying javadocs from $generated_doc_path to $target_path"
       mkdir -p "$target_path"
       cp -r "$generated_doc_path/." "$target_path/"
       copied=$((copied + 1))
     done < <(find "$MAIN_DIRECTORY_LOCATION/$module" -type d -path "*/$doc_subpath")
   done
   if [ "$copied" -eq 0 ]; then
     echo "ERROR: no javadocs were generated for module '$module'; looked for ${doc_subpaths[*]}." >&2
     exit 1
   fi
 done
