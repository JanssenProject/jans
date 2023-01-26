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
  read -rp "Enter persistence type [LDAP(NOT SUPPORTED YET)|MYSQL|PGSQL]:                            " JANS_PERSISTENCE
fi

if [[ -z $EXT_IP ]]; then
  EXT_IP=$(dig +short myip.opendns.com @resolver1.opendns.com)
fi

sudo apt-get update
# Install Docker and Docker compose plugin
sudo apt-get remove docker docker-engine docker.io containerd runc -y || echo "Docker doesn't exist..installing.."
sudo apt-get update
sudo apt-get install \
  ca-certificates \
  curl \
  gnupg \
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
    && git checkout main \
    && git sparse-checkout set docker-jans-monolith \
    && cd "$WORKING_DIRECTORY"

# -- Parse compose and docker file
sudo apt-get update
sudo python3 -m pip install --upgrade pip
pip3 install setuptools --upgrade
pip3 install dockerfile-parse ruamel.yaml
if [[ "$JANS_BUILD_COMMIT" ]]; then
  python3 -c "from dockerfile_parse import DockerfileParser ; dfparser = DockerfileParser('/tmp/jans/docker-jans-monolith') ; dfparser.envs['JANS_SOURCE_VERSION'] = '$JANS_BUILD_COMMIT'"
fi
python3 -c "from pathlib import Path ; import ruamel.yaml ; compose = Path('/tmp/jans/docker-jans-monolith/jans-mysql-compose.yml') ; yaml = ruamel.yaml.YAML() ; data = yaml.load(compose) ; data['services']['jans']['build'] = '.' ; del data['services']['jans']['image'] ; yaml.dump(data, compose)"
python3 -c "from pathlib import Path ; import ruamel.yaml ; compose = Path('/tmp/jans/docker-jans-monolith/jans-postgres-compose.yml') ; yaml = ruamel.yaml.YAML() ; data = yaml.load(compose) ; data['services']['jans']['build'] = '.' ; del data['services']['jans']['image'] ; yaml.dump(data, compose)"
# --
if [[ $JANS_PERSISTENCE == "MYSQL" ]]; then
  docker compose -f /tmp/jans/docker-jans-monolith/jans-mysql-compose.yml up -d
elif [[ $JANS_PERSISTENCE == "PGSQL" ]]; then
  docker compose -f /tmp/jans/docker-jans-monolith/jans-postgres-compose.yml up -d
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
echo "Will be ready in exactly 3 mins"
sleep 180
cat << EOF > testendpoints.sh
echo -e "Testing openid-configuration endpoint.. \n"
docker exec docker-jans-monolith-jans-1 curl -f -k https://localhost/.well-known/openid-configuration
echo -e "Testing scim-configuration endpoint.. \n"
docker exec docker-jans-monolith-jans-1 curl -f -k https://localhost/.well-known/scim-configuration
echo -e "Testing fido2-configuration endpoint.. \n"
docker exec docker-jans-monolith-jans-1 curl -f -k https://localhost/.well-known/fido2-configuration
EOF
sudo bash testendpoints.sh
echo -e "You may re-execute bash testendpoints.sh to do a quick test to check the configuration endpoints."
echo -e "Add the following record to your local computers' hosts file to engage with the services $EXT_IP $JANS_FQDN"
echo -e "To clean up run:"
echo -e "docker compose -f /tmp/jans/docker-jans-monolith/jans-mysql-compose.yml down && rm -rf /tmp/jans"
echo -e "or docker compose -f /tmp/jans/docker-jans-monolith/jans-postgres-compose.yml down && rm -rf /tmp/jans"
