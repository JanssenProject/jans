#!/bin/sh

warName=jans-client-api-server-5.0.0-SNAPSHOT-distribution.zip
distDir=oxd-dist

# Clean up
rm -f $warName
rm -f -r $destDir

# Download and unzip
wget http://ox.gluu.org/maven/org/xdi/jans-client-api/5.0.0-SNAPSHOT/$warName
unzip $warName -d $distDir