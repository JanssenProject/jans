wget http:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*ubuntu20.04_amd64.deb' | head -n 1) -O ~/jans.ubuntu20.04_amd64.deb
apt install -y ~/jans.ubuntu20.04_amd64.deb
python3 /opt/jans/jans-setup/setup.py

wget https://ox.gluu.org/icrby8xcvbcv/cli-swagger/jca_swagger_client.zip
unzip jca_swagger_client.zip
cp -r -v jca_swagger_client-0.0.1/jca/ /opt/jans/jans-cli/
