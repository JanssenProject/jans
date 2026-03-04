#!/bin/bash
set -euo pipefail

VERSION=$(echo "%VERSION%" | awk -F '-' '{print $1}')
REL=$(echo "%VERSION%" | sed "s/^${VERSION}//g" | sed "s/^-//g")
if [ -z "$REL" ]; then
        RELEASE="el10"
else
        RELEASE="$REL.el10"
fi

if [ ! -d "rpmbuild/RPMS/x86_64" ]; then
        echo "ERROR: rpmbuild/RPMS/x86_64 directory not found"
        exit 1
fi

pushd rpmbuild/RPMS/x86_64
trap 'popd' EXIT

echo "VERSION: $VERSION"
echo "RELEASE: $RELEASE"

RPM_FILE="jans-${VERSION}-${RELEASE}.x86_64.rpm"
if [ ! -f "$RPM_FILE" ]; then
        echo "ERROR: Expected RPM file $RPM_FILE not found"
        ls -la *.rpm 2>/dev/null || echo "No RPM files in directory"
        exit 1
fi

echo "Creating checksum file for release build"
sha256sum "$RPM_FILE" > "${RPM_FILE}.sha256sum"
