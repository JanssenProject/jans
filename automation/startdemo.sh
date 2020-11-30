#!/bin/bash
set -e
sudo apt-get update
sudo apt-get install python3-pip -y
sudo pip3 install setuptools --upgrade
sudo pip3 install pyOpenSSL --upgrade
sudo apt-get update
sudo apt-get install build-essential unzip -y
sudo pip3 install requests --upgrade
sudo pip3 install shiv
git clone https://github.com/JanssenProject/jans-cloud-native.git || [ -d "./jans-cloud-native" ] && echo "Directory exists."
cd jans-cloud-native
sudo snap install microk8s --classic
sudo microk8s.status --wait-ready
sudo microk8s.enable dns registry ingress
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
microk8s.kubectl create namespace jans
microk8s.config > ~/.kube/config
default_iface=$(awk '$2 == 00000000 { print $1 }' /proc/net/route)
ip=$(ip addr show dev "$default_iface" | awk '$1 == "inet" { sub("/.*", "", $2); print $2 }')
helm install jans -f ./jans-cloud-native/helm/values.yaml ./jans-cloud-native/helm -n jans --set global.lbIp="$ip" || echo "Please get ip of the instance and run helm install jans -f ./jans-cloud-native/helm/values.yaml ./jans-cloud-native/helm -n jans --set global.lbIp=<ip>"
