#!/bin/sh

warName=oxd-server-4.0.b1-distribution.zip
distDir=oxd-dist

# Clean up
rm -f $warName
rm -f -r $destDir

# Download and unzip
wget http://ox.gluu.org/maven/org/xdi/oxd-server/4.0.b1/$warName
unzip $warName -d $distDir