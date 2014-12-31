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

setuppy_path=/install/community-edition-setup-version_1.7
gluu_updates_path=/opt/gluu-updates
webapps_path=/opt/tomcat/webapps
backup=/opt/gluu-backup
opendj_path=/opt/opendj/config/schema
update_log=/opt/gluu-updates/update.log

if [ ! -f $setuppy_path/setup.log ] || [ "`cat $setuppy_path/setup.log |grep setup.properties.last`" = "" ]; then
    echo "Please first install setup.py" 2>&1 |tee -a $update_log
         exit 1
fi


update_names=(`ls $gluu_updates_path |grep -Eohw 'identity|oxauth|idp|cas|oxasimba'`)
ldif_names=(`ls $gluu_updates_path |grep -Eohw '101-ox.ldif'`)
webapps_names=(`ls $webapps_path   | grep .war | grep -Eohw 'identity|oxauth|idp|cas|oxasimba'`)
combined=( "${update_names[@]}" "${webapps_names[@]}" )
names=(`printf '%s\n' "${combined[@]}" | sort| uniq -d`)

prompt() {

local var_type=$1
local chk=$1


echo -en "Do you want update $var_type [Yes/No]: " 2>&1 |tee -a $update_log
read var_type

echo -en "$var_type \n" >> $update_log
until echo "$var_type" | grep -Eixq "[yn]|yes|no"
    do
	echo -en 'Invalid input\nReinput correct answer [Yes/No]: ' 2>&1 |tee -a $update_log
	read var_type
	echo -en "$var_type \n" >> $update_log
    done


case $var_type in

	[yY] | [yY][Ee][Ss] )
		
		if [ "$chk" != "gluu-server" ]; then
                agreed[i]=$chk
                echo -en "$chk \n" >> $update_log
                echo -en "$var_type \n" >> $update_log
		fi
                ;;

        [nN] | [nN][Oo] )
		
		echo -en "$chk \n" >> $update_log
                echo -en "$var_type \n" >> $update_log
		
		if [ "$chk" = "gluu-server" ]; then
		    
            	    echo -en "Updating procedure was canceled \n" 2>&1 |tee $update_log
            	    exit 1
            	fi
                ;;
        *) echo -ne "Invalid input \n" 2>&1 |tee -a $update_log
            ;;
esac


}

prompt gluu-server

i=0

for name in "${names[@]}"
    do

	prompt "$name"

	i=$i+1
    done


if [ "${#agreed[@]}" = 0 ]; then
    echo -ne "You didn't choose components which should be updated\nUpdating procedure was canceled \n" 2>&1 |tee -a $update_log
    exit 1
else
agreed=( "${agreed[@]}" "${ldif_names[@]}" )
echo  "-----------------------------------------------------------"
fi

#: <<'END'
if [ "`/usr/bin/pgrep -l 'java|httpd'`" != "" ]; then
echo -ne "Stopping Gluu-Server components.... \n" 2>&1 |tee -a $update_log
/sbin/service httpd stop >> $update_log
sleep 1
/sbin/service tomcat stop >> $update_log
sleep 1
/sbin/service opendj stop >> $update_log
sleep 1
/bin/rm -rf /opt/tomcat/work/*
/bin/mkdir -p $backup
fi

sleep 7

status=(`ps aux |grep  -Eohw 'tomcat|httpd|opendj' | sort| uniq -d`)

if [ "${#status[@]}" = 0 ]; then
    echo -ne "Services were successfully stoped \n" 2>&1 |tee -a $update_log
    echo -ne "Starting Gluu-server update... \n" 2>&1 |tee -a $update_log



for filename in "${agreed[@]}"
    do
	case $filename in
	    identity)
		echo -ne "Starting $filename  update... \n" 2>&1 |tee -a $update_log
		/bin/mkdir -p $backup/webapps_backup  2>&1 |tee -a $update_log
		/bin/mv $webapps_path/identity*  $backup/webapps_backup  2>&1 |tee -a $update_log
		/bin/cp $gluu_updates_path/identity* $webapps_path  2>&1 |tee -a $update_log
	    ;;
	    oxauth)
		echo -ne "Starting $filename  update... \n" 2>&1 |tee -a $update_log
		/bin/mkdir -p $backup/webapps_backup  2>&1 |tee -a $update_log
		/bin/mv $webapps_path/oxauth*  $backup/webapps_backup  2>&1 |tee -a $update_log
		/bin/cp $gluu_updates_path/oxauth* $webapps_path  2>&1 |tee -a $update_log
	    ;;
	    cas)
		/bin/mkdir -p $backup/webapps_backup  2>&1 |tee -a $update_log
		/bin/mv $webapps_path/cas*  $backup/webapps_backup  2>&1 |tee -a $update_log
		/bin/cp $gluu_updates_path/cas* $webapps_path  2>&1 |tee -a $update_log
	    ;;
	    oxasimba)
		echo -ne "Starting $filename  update... \n" 2>&1 |tee -a $update_log
		/bin/mkdir -p $backup/webapps_backup  2>&1 |tee -a $update_log
		/bin/mv $webapps_path/oxasimba*  $backup/webapps_backup  2>&1 |tee -a $update_log
		/bin/cp $gluu_updates_path/oxasimba* $webapps_path  2>&1 |tee -a $update_log
	    ;;
	    101-ox.ldif)
		echo -ne "Starting $filename  update... \n" 2>&1 |tee -a $update_log
		/bin/mkdir -p $backup/webapps_backup  2>&1 |tee -a $update_log
		/opt/opendj/bin/backup --backupAll --backupDirectory $backup/webapps_backup/opendj-`date "+%Y_%m_%d-%H.%M.%S"`_backup >>  $update_log
		/bin/cp -f $gluu_updates_path/101-ox.ldif $opendj_path  2>&1 |tee -a $update_log
	    ;;
	    *)
	    ;;
	esac
    done

/bin/mv $backup/webapps_backup $backup/webapps_`date "+%Y_%m_%d-%H.%M.%S"`_backup 2>&1 |tee -a $update_log
/bin/chown -R tomcat:tomcat $webapps_path 2>&1 |tee -a $update_log
/bin/chown -R ldap:ldap $opendj_path  2>&1 |tee -a $update_log

echo -ne "Starting Gluu-Server components.... \n" 2>&1 |tee -a $update_log

/sbin/service httpd start >> $update_log
/sbin/service tomcat start >> $update_log
/sbin/service opendj start >> $update_log

else
    echo -ne "Some components were not stopped \n" 2>&1 |tee -a $update_log
    echo -ne "Please see $update log\nand stop hanged services manually\nand start update.sh once more\n" 2>&1 |tee -a $update_log
    exit 1
fi

sleep 5

echo  "-----------------------------------------------------------"
status=(`ps aux |grep  -Eohw 'tomcat|httpd|opendj' | sort| uniq -d`)

if [ "${#status[@]}" = 3 ]; then
    echo -ne "Gluu-Server was seccessfully started \n" 2>&1 |tee -a $update_log
    echo -ne "You can find the backup of your opendj and webapps in $backup \n" 2>&1 |tee -a $update_log /opt/gluu-updates/update.log
    echo -ne "For more info see: $update_log  \n" 2>&1 |tee -a $update_log
    echo -ne " \n" 2>&1 |tee -a $update_log
else
    echo -ne "Some components were not started \n" 2>&1 |tee -a $update_log
    exit 1
fi


date

#END
