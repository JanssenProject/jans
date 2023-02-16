#!/bin/bash
set -euo pipefail
JVM_PROJECTS="jans-auth-server jans-orm jans-config-api jans-scim jans-core jans-notify jans-fido2 jans-eleven agama"
for module in $JVM_PROJECTS
 do
   echo "Generating javadocs for module: $module and all it's sub-modules"
   mvn -f "$module"/pom.xml javadoc:javadoc

   echo "Move generated javadocs to respective doc site location"

   echo "getting locations where javadocs got generated"
   mapfile -t generated_doc_paths < <(find "$module" -type d  -path '*/target/site/apidocs' | sed 's/\/target\/site\/apidocs//')

   echo "move javadocs from each location to respective documentation site location"
   for source_path in "${generated_doc_paths[@]}"
   do
     # check if the directory `docs/admin/reference/javadocs/$source_path` exists, if not then create one
     mkdir -p docs/admin/reference/javadocs/"$source_path"
     echo "copy javadocs for $source_path"
     cp -rv ./"$source_path"/target/site/apidocs/* ./docs/admin/reference/javadocs/"$source_path"/
   done
 done
