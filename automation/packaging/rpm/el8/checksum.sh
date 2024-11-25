#!/bin/bash

VERSION=$(echo "%VERSION%" | awk -F '-' {'print $1'})
REL=$(echo "%VERSION%" | sed "s/^${VERSION}//g" | sed "s/^-//g")
if [ -z "$REL" ]; then
        RELEASE="el8"
else
        RELEASE="$REL.el8"
fi
pushd rpmbuild/RPMS/x86_64

if [[ $VERSION == "0.0.0-nightly" ]]; then
  sha256sum jans-$VERSION-$RELEASE.x86_64.rpm > jans-0.0.0-nightly-el8.x86_64.rpm.sha256sum
else
  sha256sum jans-$VERSION-$RELEASE.x86_64.rpm > jans-$VERSION-$RELEASE.x86_64.rpm.sha256sum
fi
popd
