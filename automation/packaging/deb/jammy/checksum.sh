#!/bin/bash
VERSION=%VERSION%
sha256sum jans_%VERSION%~ubuntu22.04_amd64.deb > jans_%VERSION%~ubuntu22.04_amd64.deb.sha256sum
sed -i 's/~/./g' jans_%VERSION%~ubuntu22.04_amd64.deb.sha256sum
