#!/bin/bash
set -euo pipefail
# The below variable represents the top level directory of the repository
MAIN_DIRECTORY_LOCATION=$1
echo "Copies generated/duplicate cn docs such as the helm main readme to the main docs folder"

echo "Generating helm docs"
# echo "Install helm docs
mkdir helmtemp
cd helmtemp
HELM_DOCS_VERSION=$(curl "https://api.github.com/repos/norwoodj/helm-docs/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/' | cut -c2-)
curl -sSL https://github.com/norwoodj/helm-docs/releases/download/v"${HELM_DOCS_VERSION}"/helm-docs_"${HELM_DOCS_VERSION}"_Linux_x86_64.tar.gz  -o helm-docs_"${HELM_DOCS_VERSION}"_Linux_x86_64.tar.gz
tar xvf helm-docs_"${HELM_DOCS_VERSION}"_Linux_x86_64.tar.gz
sudo cp helm-docs /usr/local/bin/
cd ..
# Generate Helm docs
helm-docs "$MAIN_DIRECTORY_LOCATION"/charts/ --skip-version-footer
rm -rf helmtemp
echo "Copying Helm chart Readme to helm-chart.md"
cp "$MAIN_DIRECTORY_LOCATION"/charts/janssen/README.md "$MAIN_DIRECTORY_LOCATION"/docs/janssen-server/reference/kubernetes/helm-chart.md
echo "Adding keywords to helm-chart"
sed -i '1 s/^/---\ntags:\n  - administration\n  - reference\n  - kubernetes\n---\n/' "$MAIN_DIRECTORY_LOCATION"/docs/janssen-server/reference/kubernetes/helm-chart.md
echo "Copying docker-monolith main README.md to compose.md"
cp "$MAIN_DIRECTORY_LOCATION"/docker-jans-monolith/README.md "$MAIN_DIRECTORY_LOCATION"/docs/janssen-server/install/docker-install/compose.md
echo "Copying docker images Readme to respective image md"
# cp docker files main README.md
docker_images="docker-jans-auth-server docker-jans-cloudtools docker-jans-config-api docker-jans-configurator docker-jans-fido2 docker-jans-persistence-loader docker-jans-scim docker-jans-monolith docker-jans-casa docker-jans-link docker-jans-all-in-one"
for image in $docker_images;do
  cp "$MAIN_DIRECTORY_LOCATION"/"$image"/README.md "$MAIN_DIRECTORY_LOCATION"/docs/janssen-server/reference/kubernetes/"$image".md
done
echo "generated-cn-docs.sh executed successfully!"
