#!/bin/bash

# The MIT License (MIT)
#
# Copyright (c) 2014 Gluu
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

# Script to compress logfiles over defined periodicity
#Author : Vishwa Ranjan
#Refactor: Zico, Oct 14, 2024

#Variable Definition
KEEPDAYS1=1   #for compression
KEEPDAYS2=15  #for deletion


DIRS="/opt/jans/jetty/jans-auth/logs /opt/jans/jetty/jans-casa/logs /opt/jans/jetty/jans-config-api/logs /opt/jans/jetty/jans-fido2/logs /opt/jans/jetty/jans-keycloak-link/logs /opt/jans/jetty/jans-link/logs /opt/jans/jetty/jans-lock/logs /opt/jans/jetty/jans-scim/logs /var/log/ /var/log/adminui"

PWD=`pwd`
PWD1=$PWD
TEMPDIR="/tmp/gluumanager"
mkdir $TEMPDIR >> /var/log/logmanager.log 2>&1
cd $TEMPDIR
echo "$(date) : Changing to TEMP Directory" $TEMPDIR >> /var/log/logmanager.log
#compression Section
echo "$(date) : Starting Compression">> /var/log/logmanager.log
for dir in $DIRS
do
 if [ -d "$dir" ]; then
 FILES=`find $dir -type f -mtime +$KEEPDAYS1 ! -name "*.gz" |xargs ls `
 fi
 for fname in $FILES
   do
           echo "$(date) : Compressing file" $fname >> /var/log/logmanager.log
           gzip --best --force $fname >> /var/log/logmanager.log 2>&1
   done
done

# Deletion Section
echo "$(date) : Starting Deletion">> /var/log/logmanager.log
for dir in $DIRS
do
 if [ -d "$dir" ]; then
 FILES=`find $dir -name "*gz" -type f -mtime +$KEEPDAYS2 |xargs ls `
 fi
 for fname in $FILES
   do
           echo "$(date) : Deleting file" $fname >> /var/log/logmanager.log
           rm -f $fname >> /var/log/logmanager.log 2>&1
   done
done
echo "$(date) : Changing back to working  Directory" $PWD1 >> /var/log/logmanager.log
cd $PWD1 >> /var/log/logmanager.log 2>&1
echo "$(date) : Removing Temp Directory" $TEMPDIR >> /var/log/logmanager.log
rm -r  $TEMPDIR >> /var/log/logmanager.log 2>&1
