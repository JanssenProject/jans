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
nginx-ingress:
  ingress:
    adminUiEnabled: false
    openidConfigEnabled: true
    uma2ConfigEnabled: true
    webfingerEnabled: true
    webdiscoveryEnabled: true
    scimConfigEnabled: false
    scimEnabled: false
    configApiEnabled: true
    u2fConfigEnabled: true
    fido2ConfigEnabled: false
    authServerEnabled: true
    path: /
    hosts:
    - $JANS_FQDN
    # -- Secrets holding HTTPS CA cert and key.
    tls:
    - secretName: tls-certificate
      hosts:
      - $JANS_FQDN
    authServerProtectedToken: true
    authServerProtectedRegister: true
    additionalAnnotations:
      nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional"
      nginx.ingress.kubernetes.io/auth-tls-secret: "gluu/ca-secret"
      nginx.ingress.kubernetes.io/auth-tls-verify-depth: "1"
      nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true"
global:
  auth-server-key-rotation:
    enabled: false
  client-api:
    enabled: false
  config-api:
    enabled: true
  fido2:
    enabled: false
  scim:
    enabled: false
  isFqdnRegistered: false
  lbIp: $EXT_IP
EOF
sudo helm repo add jans https://gluufederation.github.io/flex/pygluu/kubernetes/templates/helm
sudo helm repo update
sudo helm install jans jans/gluu -n jans --version=5.0.2 -f override.yaml --kubeconfig="$KUBECONFIG"
echo "Waiting for auth-server to come up....Please do not cancel out...This will wait for the auth-server to be ready.."
sleep 120
cat << EOF > testendpoints.sh
# get certs and keys. This will also generate the client crt and key to be used to access protected endpoints
mkdir quicktestcerts || echo "directory exists"
cd quicktestcerts
sudo microk8s config > config
KUBECONFIG="$PWD"/config
rm ca.crt ca.key server.crt server.key client.csr client.crt client.key
sudo microk8s.kubectl delete secret generic ca-secret -n gluu --kubeconfig="$KUBECONFIG" || echo "secret ca-secret does not exist and will be created."
sudo microk8s.kubectl get secret cn -o json -n gluu --kubeconfig="$KUBECONFIG" | grep '"ssl_ca_cert":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d > ca.crt
sudo microk8s.kubectl get secret cn -o json -n gluu --kubeconfig="$KUBECONFIG" | grep '"ssl_ca_key":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d > ca.key
sudo microk8s.kubectl get secret cn -o json -n gluu --kubeconfig="$KUBECONFIG" | grep '"ssl_cert":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d > server.crt
sudo microk8s.kubectl get secret cn -o json -n gluu --kubeconfig="$KUBECONFIG" | grep '"ssl_key":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d > server.key
openssl req -new -newkey rsa:4096 -keyout client.key -out client.csr -nodes -subj '/CN=Openbanking'
openssl x509 -req -sha256 -days 365 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 02 -out client.crt
sudo microk8s.kubectl create secret generic ca-secret -n gluu --from-file=tls.crt=server.crt --from-file=tls.key=server.key --from-file=ca.crt=ca.crt
echo -e "Starting simple test to endpoints. \n"
sleep 10
echo -e "Testing openid-configuration endpoint.. \n"
curl -k https://demoexample.gluu.org/.well-known/openid-configuration
TESTCLIENT=$(microk8s.kubectl get cm cn -o json -n gluu --kubeconfig="$KUBECONFIG" | grep '"jca_client_id":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]')
TESTCLIENTSECRET=$(microk8s.kubectl get secret cn -o json -n gluu --kubeconfig="$KUBECONFIG" | grep '"jca_client_pw":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d)
echo -e "Testing protected endpoint /token without client crt and key. This should show a 403, showing mTLS works \n"
curl -X POST -k -u $TESTCLIENT:$TESTCLIENTSECRET https://demoexample.gluu.org/jans-auth/restv1/token -d grant_type=client_credentials
echo -e "Testing protected endpoint /token with client crt and key. This should recieve a token, showing mTLS works \n"
curl -X POST -k --cert client.crt --key client.key -u $TESTCLIENT:$TESTCLIENTSECRET https://demoexample.gluu.org/jans-auth/restv1/token -d grant_type=client_credentials
echo -e "Testing protected endpoint /register without client crt and key. This should show a 403, showing mTLS works \n"
curl -X POST -k -u $TESTCLIENT:$TESTCLIENTSECRET https://demoexample.gluu.org/jans-auth/restv1/register
echo -e "Testing protected endpoint /register with client crt and key. This should still recieve an error but from the AS showing mTLS works \n"
curl -X POST -k --cert client.crt --key client.key -u $TESTCLIENT:$TESTCLIENTSECRET https://demoexample.gluu.org/jans-auth/restv1/register
cd ..
EOF
sudo microk8s.kubectl -n jans wait --for=condition=available --timeout=600s deploy/jans-auth-server --kubeconfig="$KUBECONFIG"
sudo bash testendpoints.sh
echo -e "You may re-execute bash testendpoints.sh to do a quick test to protected endpoints and openid-configuration endpoint."
