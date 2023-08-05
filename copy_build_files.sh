#!/bin/bash

IN_BASE_DIR=".";
OUT_DIR="./_out";
OUT_PLUGINS_DIR="${OUT_DIR}/_plugins";

SRC_FPATHS=(
    "${IN_BASE_DIR}/jans-auth-server/server/target/jans-auth-server.war"
    "${IN_BASE_DIR}/jans-auth-server/server-fips/target/jans-auth-server-fips.war"
    "${IN_BASE_DIR}/jans-auth-server/client/target/jans-auth-client-jar-with-dependencies.jar"
    "${IN_BASE_DIR}/jans-auth-server/client/target/jans-auth-client-jar-without-provider-dependencies.jar"
    "${IN_BASE_DIR}/jans-eleven/server/target/eleven.war"
    "${IN_BASE_DIR}/jans-eleven/server-fips/target/jans-eleven-server-fips.war"
    "${IN_BASE_DIR}/jans-fido2/server/target/fido2-server.war"
    "${IN_BASE_DIR}/jans-fido2/server-fips/target/jans-fido2-server-fips.war"
    "${IN_BASE_DIR}/jans-fido2/client/target/Fido2-Client.jar"
    "${IN_BASE_DIR}/jans-scim/server/target/jans-scim-server-1.0.16-SNAPSHOT.war"
    "${IN_BASE_DIR}/jans-scim/server-fips/target/jans-scim-server-fips.war"
    "${IN_BASE_DIR}/jans-config-api/server/target/jans-config-api.war"
    "${IN_BASE_DIR}/jans-config-api/server-fips/target/jans-config-api-server-fips.war"
    "${IN_BASE_DIR}/jans-config-api/plugins/admin-ui-plugin/target/admin-ui-plugin-distribution.jar"
    "${IN_BASE_DIR}/jans-config-api/plugins/fido2-plugin/target/fido2-plugin-1.0.16-SNAPSHOT-distribution.jar"
    "${IN_BASE_DIR}/jans-config-api/plugins/scim-plugin/target/scim-plugin-1.0.16-SNAPSHOT-distribution.jar"
    "${IN_BASE_DIR}/jans-config-api/plugins/user-mgt-plugin/target/user-mgt-plugin-1.0.16-SNAPSHOT-distribution.jar"
    "${IN_BASE_DIR}/jans-orm/spanner-libs/target/jans-orm-spanner-libs-1.0.16-SNAPSHOT-distribution.zip"
    "${IN_BASE_DIR}/jans-link/server/target/jans-link-server-1.0.16-SNAPSHOT.war"
    "${IN_BASE_DIR}/jans-link/server-fips/target/jans-link-server-fips.war"
    "${IN_BASE_DIR}/jans-link/service/target/jans-link-service-1.0.16-SNAPSHOT.jar"
);

DST_FPATHS=(
    "${OUT_DIR}/jans-auth-server.war"
    "${OUT_DIR}/jans-auth-server-fips.war"
    "${OUT_DIR}/jans-auth-client-jar-with-dependencies.jar"
    "${OUT_DIR}/jans-auth-client-jar-without-provider-dependencies.jar"
    "${OUT_DIR}/eleven.war"
    "${OUT_DIR}/jans-eleven-server-fips.war"
    "${OUT_DIR}/fido2-server.war"
    "${OUT_DIR}/jans-fido2-server-fips.war"
    "${OUT_DIR}/Fido2-Client.jar"
    "${OUT_DIR}/jans-scim-server-1.0.16-SNAPSHOT.war"
    "${OUT_DIR}/jans-scim-server-fips.war"
    "${OUT_DIR}/jans-config-api.war"
    "${OUT_DIR}/jans-config-api-server-fips.war"
    "${OUT_PLUGINS_DIR}/admin-ui-plugin-distribution.jar"
    "${OUT_PLUGINS_DIR}/fido2-plugin-1.0.16-SNAPSHOT-distribution.jar"
    "${OUT_PLUGINS_DIR}/scim-plugin-1.0.16-SNAPSHOT-distribution.jar"
    "${OUT_PLUGINS_DIR}/user-mgt-plugin-1.0.16-SNAPSHOT-distribution.jar"
    "${OUT_DIR}/jans-orm-spanner-libs-1.0.16-SNAPSHOT-distribution.zip"
    "${OUT_DIR}/jans-link-server-1.0.16-SNAPSHOT.war"
    "${OUT_DIR}/jans-link-server-fips.war"
    "${OUT_DIR}/jans-link-service-1.0.16-SNAPSHOT.jar"
);

NUM_ALL=${#SRC_FPATHS[@]};

NUM_OK=0;
NUM_ERROR=0;

echo "--------------------------------------";
echo "Removing: ${OUT_DIR}";

if [ -d ${OUT_DIR} ];
then
	rm -f -r ${OUT_DIR};
fi;

echo "--------------------------------------";
echo "Creating Directory: ${OUT_DIR}";

if [ ! -d ${OUT_DIR} ];
then
	mkdir ${OUT_DIR};
fi;

echo "--------------------------------------";
echo "Creating Directory: ${OUT_PLUGINS_DIR}";

if [ ! -d ${OUT_PLUGINS_DIR} ];
then
	mkdir ${OUT_PLUGINS_DIR};
fi;

function copying_file()
{  
    local src_fpath="$1";
    local dst_fpath="$2";
    echo "Copying: ${src_fpath} to ${dst_fpath}";
    if [ -f $file ]; then
        cp -v -f "${src_fpath}" "${dst_fpath}";
        if [ $? -eq 0 ]
        then
            echo "OK";
            return 0;
        else
            echo "Error";
            return 1;
        fi;
    else
		echo "!!!!! Error !!!! ${src_fpath} not found";
    fi;
};

echo "--------------------------------------";
echo "Copying ${#SRC_FPATHS[@]} files";

for i in ${!SRC_FPATHS[@]}; do
    echo "--------------------------------------";
    echo "SRC_FPATHS[$i] = ${SRC_FPATHS[$i]}";
    echo "DST_FPATHS[$i] = ${DST_FPATHS[$i]}";
    copying_file ${SRC_FPATHS[$i]} ${DST_FPATHS[$i]};
    if [ $? -eq 0 ]
    then
        let NUM_OK=$NUM_OK+1;
    else
        let NUM_ERROR=$NUM_ERROR+1;
    fi;
    echo "--------------------------------------";
done;

echo "--------------------------------------";
echo "Result:";
echo "NUM ALL    = ${NUM_ALL}";
echo "NUM OK     = ${NUM_OK}";
echo "NUM ERROR  = ${NUM_ERROR}";
echo "--------------------------------------";

exit 0;
