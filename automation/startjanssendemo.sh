#!/bin/bash
set -e
if [[ ! "$JANS_FQDN" ]]; then
  read -rp "Enter Hostname [demoexample.jans.io]:                           " JANS_FQDN
fi
if ! [[ $JANS_FQDN == *"."*"."* ]]; then
  echo "[E] Hostname provided is invalid or empty.
    Please enter a FQDN with the format demoexample.jans.io"
  exit 1
fi
sudo apt-get update
sudo apt-get install python3-pip -y
sudo pip3 install pip --upgrade
sudo pip3 install setuptools --upgrade
sudo pip3 install pyOpenSSL --upgrade
sudo apt-get update
sudo apt-get install build-essential unzip -y
sudo pip3 install requests --upgrade
sudo pip3 install shiv
sudo snap install microk8s --classic
sudo microk8s.status --wait-ready
sudo microk8s.enable dns registry ingress
sudo microk8s kubectl get daemonset.apps/nginx-ingress-microk8s-controller -n ingress -o yaml | sed -s "s@ingress-class=public@ingress-class=nginx@g" | microk8s kubectl apply -f -
sudo apt-get update
sudo apt-get install apt-transport-https ca-certificates curl gnupg-agent software-properties-common -y
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install net-tools
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
sudo apt-get install docker-ce docker-ce-cli containerd.io -y
sudo microk8s config > config
KUBECONFIG="$PWD"/config
sudo microk8s.kubectl create namespace jans --kubeconfig="$KUBECONFIG" || echo "namespace exists"
sudo helm repo add bitnami https://charts.bitnami.com/bitnami
sudo microk8s.kubectl get po --kubeconfig="$KUBECONFIG"
sudo helm install my-release --set auth.rootPassword=Test1234#,auth.database=jans bitnami/mysql -n jans --kubeconfig="$KUBECONFIG"
EXT_IP=$(dig +short myip.opendns.com @resolver1.opendns.com)
sudo echo "$EXT_IP $JANS_FQDN" >> /etc/hosts
cat << EOF > override.yaml
config:
  countryCode: US
  email: support@gluu.org
  orgName: Gluu
  city: Austin
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 3306
    cnSqlDbDialect: mysql
    cnSqlDbHost: my-release-mysql.jans.svc
    cnSqlDbUser: root
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#
global:
  auth-server-key-rotation:
    enabled: true
  client-api:
    enabled: true
  config-api:
    enabled: true
  fido2:
    enabled: true
  scim:
    enabled: true
  fqdn: $JANS_FQDN
  isFqdnRegistered: false
  lbIp: $EXT_IP
# -- Nginx ingress definitions chart
nginx-ingress:
  ingress:
    adminUiEnabled: false
    openidConfigEnabled: true
    uma2ConfigEnabled: true
    webfingerEnabled: true
    webdiscoveryEnabled: true
    scimConfigEnabled: true
    scimEnabled: true
    configApiEnabled: true
    u2fConfigEnabled: true
    fido2ConfigEnabled: true
    authServerEnabled: true
    path: /
    hosts:
    - $JANS_FQDN
    # -- Secrets holding HTTPS CA cert and key.
    tls:
    - secretName: tls-certificate
      hosts:
      - $JANS_FQDN
EOF
sudo helm repo add jans https://gluufederation.github.io/flex/flex-cn-setup/pygluu/kubernetes/templates/helm
sudo helm repo update
sudo helm install jans jans/gluu -n jans --version=5.0.2 -f override.yaml --kubeconfig="$KUBECONFIG"
echo "Waiting for auth-server to come up. This may take 5-10 mins....Please do not cancel out...This will wait for the auth-server to be ready.."
sleep 120
cat << EOF > testendpoints.sh
sudo microk8s config > config
KUBECONFIG="$PWD"/config
sleep 10
echo -e "Testing openid-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/openid-configuration
echo -e "Testing scim-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/scim-configuration
echo -e "Testing fido2-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/fido2-configuration
cd ..
EOF
sudo microk8s.kubectl -n jans wait --for=condition=available --timeout=600s deploy/jans-auth-server --kubeconfig="$KUBECONFIG"
sudo bash testendpoints.sh
echo -e "You may re-execute bash testendpoints.sh to do a quick test to check the openid-configuration endpoint."
