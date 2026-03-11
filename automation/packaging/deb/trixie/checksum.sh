#!/bin/bash
VERSION=%VERSION%
sha256sum jans_"${VERSION}"~debian13_amd64.deb > jans_"${VERSION}"~debian13_amd64.deb.sha256sum
sed -i 's/~/./g' jans_"${VERSION}"~debian13_amd64.deb.sha256sum
