#!/bin/bash
set -eo pipefail

JANS_FQDN=$1
JANS_PERSISTENCE=$2
EXT_IP=$3
# commit to build jans off
JANS_BUILD_COMMIT=$4
if [[ ! "$JANS_FQDN" ]]; then
  read -rp "Enter Hostname [demoexample.jans.io]:                           " JANS_FQDN
fi
if [[ ! "$JANS_PERSISTENCE" ]]; then
  read -rp "Enter persistence type [LDAP|MYSQL|PGSQL]:                            " JANS_PERSISTENCE
fi

if [[ -z $EXT_IP ]]; then
  EXT_IP=$(curl ipinfo.io/ip)
fi

wait_for_services() {
  code=404
  while [[ "$code" != "200" ]]; do
    echo "Waiting for https://${JANS_FQDN}/$1 to respond with 200"
    code=$(curl -s -o /dev/null -w ''%{http_code}'' -k https://"${JANS_FQDN}"/"$1")
    sleep 5
  done
}

sudo apt-get update
# Install Docker and Docker compose plugin
sudo apt-get remove docker docker-engine docker.io containerd runc -y || echo "Docker doesn't exist..installing.."
sudo apt-get update
sudo apt-get install \
  ca-certificates \
  curl \
  gnupg \
  python3-pip \
  lsb-release -y
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin -y
WORKING_DIRECTORY=$PWD
# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the docker-jans-monolith code
rm -rf /tmp/jans || echo "/tmp/jans doesn't exist"
git clone --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout "$JANS_BUILD_COMMIT" \
    && git sparse-checkout set docker-jans-monolith \
    && cd "$WORKING_DIRECTORY"

# -- Parse compose and docker file
sudo apt-get update
sudo python3 -m pip install --upgrade pip
pip3 install setuptools --upgrade
pip3 install dockerfile-parse ruamel.yaml

# switching to version defined by JANS_BUILD_COMMIT
if [[ "$JANS_BUILD_COMMIT" ]]; then
  python3 -c "from dockerfile_parse import DockerfileParser ; dfparser = DockerfileParser('/tmp/jans/docker-jans-monolith') ; dfparser.envs['JANS_SOURCE_VERSION'] = '$JANS_BUILD_COMMIT'"

  # as JANS_SOURCE_VERSION is changed, allow docker compose to rebuild image on-the-fly
  # and use the respective image instead of the default image
  python3 -c "from pathlib import Path ; import ruamel.yaml ; compose = Path('/tmp/jans/docker-jans-monolith/jans-mysql-compose.yml') ; yaml = ruamel.yaml.YAML() ; data = yaml.load(compose) ; data['services']['jans']['build'] = '.' ; del data['services']['jans']['image'] ; yaml.dump(data, compose)"
  python3 -c "from pathlib import Path ; import ruamel.yaml ; compose = Path('/tmp/jans/docker-jans-monolith/jans-postgres-compose.yml') ; yaml = ruamel.yaml.YAML() ; data = yaml.load(compose) ; data['services']['jans']['build'] = '.' ; del data['services']['jans']['image'] ; yaml.dump(data, compose)"
  python3 -c "from pathlib import Path ; import ruamel.yaml ; compose = Path('/tmp/jans/docker-jans-monolith/jans-ldap-compose.yml') ; yaml = ruamel.yaml.YAML() ; data = yaml.load(compose) ; data['services']['jans']['build'] = '.' ; del data['services']['jans']['image'] ; yaml.dump(data, compose)"
fi
# --
if [[ $JANS_PERSISTENCE == "MYSQL" ]]; then
  bash /tmp/jans/docker-jans-monolith/up.sh mysql
elif [[ $JANS_PERSISTENCE == "PGSQL" ]]; then
  bash /tmp/jans/docker-jans-monolith/up.sh postgres
elif [[ $JANS_PERSISTENCE == "LDAP" ]]; then
  bash /tmp/jans/docker-jans-monolith/up.sh ldap
fi  
echo "$EXT_IP $JANS_FQDN" | sudo tee -a /etc/hosts > /dev/null
jans_status="unhealthy"
# run loop for 5 mins
echo "Waiting for the Janssen server to come up. Depending on the  resources it may take 3-5 mins for the services to be up."
end=$((SECONDS+300))
while [ $SECONDS -lt $end ]; do
    jans_status=$(docker inspect --format='{{json .State.Health.Status}}' docker-jans-monolith-jans-1) || echo "unhealthy"
    echo "$jans_status"
    if [ "$jans_status" == '"healthy"' ]; then
        break
    fi
    sleep 10
    docker ps
    docker logs docker-jans-monolith-jans-1 || echo "Container is not starting..."
done
if [ "$jans_status" == '"unhealthy"' ]; then
    docker ps
    docker logs docker-jans-monolith-jans-1
    exit 1
fi
wait_for_services jans-config-api/api/v1/health/ready
wait_for_services jans-scim/sys/health-check
wait_for_services jans-fido2/sys/health-check

cat << EOF > testendpoints.sh
echo -e "Testing openid-configuration endpoint.. \n"
docker exec docker-jans-monolith-jans-1 curl -f -k https://localhost/.well-known/openid-configuration
echo -e "Testing scim-configuration endpoint.. \n"
docker exec docker-jans-monolith-jans-1 curl -f -k https://localhost/.well-known/scim-configuration
echo -e "Testing fido2-configuration endpoint.. \n"
docker exec docker-jans-monolith-jans-1 curl -f -k https://localhost/.well-known/fido2-configuration
mkdir -p /tmp/reports || echo "reports folder exists"
while ! docker exec docker-jans-monolith-jans-1 test -f "/tmp/httpd.crt"; do
  echo "Waiting for the container to run java test preparations"
  sleep 5
done
echo -e "Running build.. \n"
docker exec -w /tmp/jans/jans-auth-server docker-jans-monolith-jans-1 mvn -Dcfg="$JANS_FQDN" -Dmaven.test.skip=true -fae clean compile install
echo -e "Running tests.. \n"
docker exec -w /tmp/jans/jans-auth-server docker-jans-monolith-jans-1 mvn -Dcfg="$JANS_FQDN" -Dmaven.test.skip=false test
echo -e "copying reports.. \n"
docker cp docker-jans-monolith-jans-1:/tmp/jans/jans-auth-server/client/target/surefire-reports/testng-results.xml /tmp/reports/$JANS_PERSISTENCE-jans-auth-client-testng-results.xml
docker cp docker-jans-monolith-jans-1:/tmp/jans/jans-auth-server/agama/model/target/surefire-reports/testng-results.xml /tmp/reports/$JANS_PERSISTENCE-jans-auth-agama-model-testng-results.xml
docker cp docker-jans-monolith-jans-1:/tmp/jans/jans-auth-server/test-model/target/surefire-reports/testng-results.xml /tmp/reports/$JANS_PERSISTENCE-jans-auth-test-model-testng-results.xml
docker cp docker-jans-monolith-jans-1:/tmp/jans/jans-auth-server/model/target/surefire-reports/testng-results.xml /tmp/reports/$JANS_PERSISTENCE-jans-auth-model-testng-results.xml

EOF
sudo bash testendpoints.sh
echo -e "You may re-execute bash testendpoints.sh to do a quick test to check the configuration endpoints."
echo -e "Add the following record to your local computers' hosts file to engage with the services $EXT_IP $JANS_FQDN"
echo -e "To stop run:"
echo -e "/tmp/jans/docker-jans-monolith/down.sh mysql"
echo -e "or /tmp/jans/docker-jans-monolith/down.sh postgres"
echo -e "To restart run:"
echo -e "/tmp/jans/docker-jans-monolith/up.sh mysql"
echo -e "or /tmp/jans/docker-jans-monolith/up.sh postgres"
echo -e "To clean up run:"
echo -e "/tmp/jans/docker-jans-monolith/clean.sh mysql && rm -rf /tmp/jans"
echo -e "or /tmp/jans/docker-jans-monolith/clean.sh postgres && rm -rf /tmp/jans"
