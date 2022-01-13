#!/bin/sh

warName=jans-client-api-server-1.0.0-SNAPSHOT-distribution.zip
distDir=oxd-dist

# Clean up
rm -f $warName
rm -f -r $destDir

# Download and unzip
wget https://maven.jans.io/maven/io/jans/jans-client-api-server/1.0.0-SNAPSHOT/$warName
unzip $warName -d $distDir