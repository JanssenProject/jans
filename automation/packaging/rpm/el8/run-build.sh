#!/bin/bash

VERSION=$(echo "%VERSION%" | awk -F '-' {'print $1'})
REL=$(echo "%VERSION%" | sed "s/^${VERSION}//g" | sed "s/^-//g")
current_dir=$PWD
sed -i "s/%VER%/$VERSION/g" jans.spec
if [ -z "$REL" ]; then
        RELEASE="el8"
else
        RELEASE="$REL.el8"
fi
sed -i "s/%RELEASE%/$RELEASE/g" jans.spec
rpmbuild_path="$current_dir/rpmbuild"
mkdir -p "$rpmbuild_path"/{BUILD,BUILDROOT,RPMS,SOURCES,SPECS,SRPMS}
specfile=jans.spec
cp "$current_dir"/$specfile "$rpmbuild_path"/SPECS/.
mv jans-src jans-"$VERSION"
tar cvfz jans-"$VERSION".tar.gz jans-$VERSION
cp jans-"$VERSION".tar.gz "$rpmbuild_path"/SOURCES/.
rm -rf rpmbuild/RPMS/x86_64/*
rpmbuild -bb --define "_topdir $rpmbuild_path" "$rpmbuild_path"/SPECS/$specfile
chmod a+w rpmbuild/RPMS/x86_64/jans-"$VERSION"-"$RELEASE".x86_64.rpm
chmod a+w rpmbuild/RPMS/x86_64
