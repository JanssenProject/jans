VERSION=1.0.12
sudo wget https://github.com/JanssenProject/jans/releases/download/v${VERSION}.nightly/jans-${VERSION}.nightly-suse15.x86_64.rpm -P /tmp
sudo wget https://github.com/JanssenProject/jans/releases/download/v${VERSION}.nightly/jans-${VERSION}.nightly-suse15.x86_64.rpm.sha256sum -P /tmp
cd /tmp
sha256sum -c jans-${VERSION}.nightly-suse15.x86_64.rpm.sha256sum

#apt install -y /tmp/jans_${VERSION}.nightly.ubuntu22.04_amd64.deb
sudo zypper install /tmp/jans-${VERSION}.nightly-suse15.x86_64.rpm
sudo python3 /opt/jans/jans-setup/setup.py

# wget https://ox.gluu.org/icrby8xcvbcv/cli-swagger/jca_swagger_client.zip
# unzip jca_swagger_client.zip
# cp -r -v jca_swagger_client-0.0.1/jca/ /opt/jans/jans-cli/

