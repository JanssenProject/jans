#!/bin/bash
set -euo pipefail

echo "Copies generated/duplicate cn docs such as the helm main readme to the main docs folder"

# Generate Helm docs
# Install helm docs
VERSION=$(curl "https://api.github.com/repos/norwoodj/helm-docs/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/' | cut -c2-)
curl -sSL https://github.com/norwoodj/helm-docs/releases/download/v"${VERSION}"/helm-docs_"${VERSION}"_Linux_x86_64.tar.gz  -o helm-docs_"${VERSION}"_Linux_x86_64.tar.gz
tar xvf helm-docs_"${VERSION}"_Linux_x86_64.tar.gz
sudo cp helm-docs /usr/local/bin/
# Generate Helm docs
helm-docs charts/
cp ./charts/README.md ./docs/admin/reference/kubernetes/helm-chart.md
sed -i '1 s/^/---\ntags:\n  - administration\n  - reference\n  - kubernetes\n---\n/' ./docs/admin/reference/kubernetes/helm-chart.md
# cp docker-monolith main README.md
cp ./docker-jans-monolith/README.md  ./docs/admin/install/docker-install/compose.md
# cp docker files main README.md
docker_images="docker-jans-auth-server docker-jans-certmanager docker-jans-config-api docker-jans-configurator docker-jans-fido2 docker-jans-persistence-loader docker-jans-scim docker-jans-monolith"
for image in $docker_images;do
  cp ./"$image"/README.md ./docs/admin/reference/kubernetes/"$image".md
done
# cp docker-opendj main README.md
wget https://raw.githubusercontent.com/GluuFederation/docker-opendj/5.0/README.md -O ./docs/admin/reference/kubernetes/docker-opendj.md
sed -i '1 s/^/---\ntags:\n  - administration\n  - reference\n  - kubernetes\n - docker image\n---\n/' ./docs/admin/reference/kubernetes/docker-opendj.md
