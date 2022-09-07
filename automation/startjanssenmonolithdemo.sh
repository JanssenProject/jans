#!/bin/bash
set -eo pipefail

JANS_FQDN=$1
JANS_PERSISTENCE=$2

if [[ ! "$JANS_FQDN" ]]; then
  read -rp "Enter Hostname [demoexample.jans.io]:                           " JANS_FQDN
fi
if [[ ! "$JANS_PERSISTENCE" ]]; then
  read -rp "Enter persistence type [LDAP(NOT SUPPORTED YET)|MYSQL]:                            " JANS_PERSISTENCE
fi

sudo apt-get update
# Install Docker and Docker compose plugin
sudo apt-get remove docker docker-engine docker.io containerd runc -y
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

# note that as we're pulling from a monorepo (with multiple project in it)
# we are using partial-clone and sparse-checkout to get the docker-jans-monolith code
git clone --filter blob:none --no-checkout https://github.com/janssenproject/jans /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout main \
    && git sparse-checkout set docker-jans-monolith \
    && cd ../../

if [[ $JANS_PERSISTENCE == "MYSQL" ]]; then
  docker-compose -f /tmp/jans/mysql-docker-compose.yml up -d
fi
echo "$EXT_IP $JANS_FQDN" | sudo tee -a /etc/hosts > /dev/null
echo "Waiting for the Janssen server to come up. This will take around 3 mins"
sleep 180
cat << EOF > testendpoints.sh
echo -e "Testing openid-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/openid-configuration
echo -e "Testing scim-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/scim-configuration
echo -e "Testing fido2-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/fido2-configuration
cd ..
EOF
sudo bash testendpoints.sh
echo -e "You may re-execute bash testendpoints.sh to do a quick test to check the configuration endpoints."
