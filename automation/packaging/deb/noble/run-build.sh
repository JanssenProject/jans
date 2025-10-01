#!/bin/bash
VERSION=%VERSION%
sed -i "s/%VER%/$VERSION/g" debian/changelog
cd jans-src
tar cvfz ../jans_%VERSION%.tar.gz *
cp -a ../debian .
tar cvfz ../jans_%VERSION%.orig.tar.gz *
debuild -us -uc
cd ..
chmod a+w jans_%VERSION%~ubuntu24.04_amd64.deb
