#!/usr/bin/bash -x
##!/usr/bin/expect  -d



LOG_LOCATION=./logs
exec > >(tee -i $LOG_LOCATION/install.log)
exec 2>&1


mkdir logs
if [ ! -d ./logs ]
then
  mkdir ./logs
fi

if [ ! -d ./report ]
then
  mkdir ./report
fi



rm  -rf  $LOG_LOCATION/*
rm  -rf ./report/result.txt



helpFunction()
{
   echo ""
   echo "Usage:  -h servername -u username -p passwd"
   echo -e "\t-host name of the host server -u username of host -p passwd of user "
   exit 1 # Exit script after printing help
}

MODULE="Attribute defaultMethods cacheconfig memcacheconf memory-cache-configue redis-cache-configuration in-Memory-cache-configuration native-persist-cacheconf Configuration-property fido2-configuration SMTP-configuration Logconfiguration LDAP-configuration couchbaseDB-configuration openID-connnect UMA OAuth-Scopes Statistics health server-Statistics scim-user-mgmt Organization-Configuration AuthServerHealth"

reportgen () {
	for i in $MODULE
	do

		if [[ `grep -ir "$i tests passed" $LOG_LOCATION/jans-cli_test.log` ]]
		then
			echo "$i Passed" >>./report/result.txt
		else
			echo "$i Failed" >>./report/result.txt

		fi
	done

os_name=`uname -v | awk {'print$1'} | cut -f2 -d'-'`
upt=`uptime | awk {'print$3'} | cut -f1 -d','`
ip_add=`ifconfig | grep "inet addr" | head -2 | tail -1 | awk {'print$2'} | cut -f2 -d:`
num_proc=`ps -ef | wc -l`
root_fs_pc=`df -h /dev/sda1 | tail -1 | awk '{print$5}'`
total_root_size=`df -h /dev/sda1 | tail -1 | awk '{print$2}'`
#load_avg=`uptime | cut -f5 -d':'`
load_avg=`cat /proc/loadavg  | awk {'print$1,$2,$3'}`
ram_usage=`free -m | head -2 | tail -1 | awk {'print$3'}`
ram_total=`free -m | head -2 | tail -1 | awk {'print$2'}`
inode=`df -i / | head -2 | tail -1 | awk {'print$5'}`
os_version=`uname -v | cut -f2 -d'~' | awk {'print$1'} | cut -f1 -d'-' | cut -c 1-5`

html="./report/test-Report-`hostname`-`date +%y%m%d`-`date +%H%M`.html"
email_add="manoj.suryawansham@gmail.com"
#for i in `ls /home`; do sudo du -sh /home/$i/* | sort -nr | grep G; done > /tmp/dir.txt
#Generating HTML file
echo "<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">" >> $html
echo "<html>" >> $html
echo "<link rel="stylesheet" href="https://unpkg.com/purecss@0.6.2/build/pure-min.css">" >> $html
echo "<body>" >> $html
echo "<fieldset>" >> $html
echo "<center>" >> $html
echo "<h2>Jans-CLI test Report" >> $html
echo "<h3><legend>scripted  by Manoj</legend></h3>" >> $html
echo "</center>" >> $html
echo "</fieldset>" >> $html
echo "<br>" >> $html
echo "<center>" >> $html
echo "<h2>OS Details : </h2>" >> $html
echo "<table class="pure-table">" >> $html
echo "<thead>" >> $html
echo "<tr>" >> $html
echo "<th>OS Name</th>" >> $html
echo "<th>OS Version</th>" >> $html
echo "<th>IP Address</th>" >> $html
echo "<th>Uptime</th>" >> $html
echo "</tr>" >> $html
echo "</thead>" >> $html
echo "<tbody>" >> $html
echo "<tr>" >> $html
echo "<td>$os_name</td>" >> $html
echo "<td>$os_version</td>" >> $html
echo "<td>$ip_add</td>" >> $html
echo "<td>$upt</td>" >> $html
echo "</tr>" >> $html
echo "</tbody>" >> $html
echo "</table>" >> $html
echo "<h2>Jans-CLI Test Details : </h2>" >> $html
echo "<br>" >> $html
echo "<table class="pure-table">" >> $html
echo "<thead>" >> $html
echo "<tr>" >> $html
echo "<th>Test Name</th>" >> $html
echo "<th>STATUS</th>" >> $html
echo "</tr>" >> $html
echo "</thead>" >> $html
echo "<tr>" >> $html
while read size name;
do
  echo "<td>$size</td>" >> $html
  echo "<td>$name</td>" >> $html
  echo "</tr>" >> $html
  echo "</tbody>" >> $html
done < ./report/result.txt 
echo "</table>" >> $html
echo "</body>" >> $html
echo "</html>" >> $html
echo "Report has been generated in report with file-name = $html. Report has also been sent to $email_add."
#Sending Email to the user
cat $html | mail -s "jans-cli test Report" -a "MIME-Version: 1.0" -a "Content-Type: text/html" -a "From: manoj <manojsurya78@gmail.com>" $email_add

}
 

unset servername username passwd
while getopts h:u:p: option
do
case "${option}"
in
h) HOST=${OPTARG};;
u) USERNAME=${OPTARG};;
p) PASSWD=${OPTARG};;
esac
done

# Print helpFunction in case parameters are empty

if [ -z ${HOST} ] && [ -z ${USERNAME} ] && [ -z ${PASSWD} ]
  then
	echo " host parameter is empty or username or passwd is not added"
	helpFunction
fi 
# Begin script in case all parameters are correct
echo "your host is $HOST"
echo "your username is ${USERNAME}"
echo "your passwd is ${PASSWD}"

#USER=(root root)


expect <<EOF
spawn ssh-keygen  -t rsa -P '' -f ~/.ssh/id_rsa
expect { "Overwrite (y/n)?" {send "y\r"} }
spawn ssh-copy-id -f -i "/home/manoj/.ssh/id_rsa.pub" ${USERNAME}@${HOST}
expect {
  "yes/no" { send "yes\n";exp_continue }
  "password:" { send "${PASSWD}\r" }

}
expect eof
EOF


HOST_OS=`ssh  ${USERNAME}@${HOST} 'cat /etc/os-release | grep PRETTY_NAME | cut -d" " -f-1,2 | cut -d"\"" -f2,3'`
echo $HOST_OS
OSTYPE=`echo $HOST_OS |cut -d" " -f1` 
echo $OSTYPE

OSVERSION=`echo $HOST_OS |cut -d" " -f2` 

echo $OSVERSION
echo $OSTYPE
	
	while true
	
       	do
		case "$OSTYPE" 

		in
			"Ubuntu")
	
				echo " login to Ubuntu Host"
				if ( ssh  ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
					then

expect<<EOF

spawn /usr/bin/ssh $USERNAME@$HOST
expect -re "(.*)"
send "python3 install.py -uninstall\r"
expect -re "(.*)"
send "yes\r"
sleep 10
expect "/opt/dist"
expect eof
EOF
				fi


				#2>&1 >$LOG_LOCATION/install.log
				./install.exp ${HOST} ${USERNAME} 2>&1 >$LOG_LOCATION/install.log
				sleep 20
				OPENIDINUM=`ssh  ${USERNAME}@${HOST} 'grep -ir "Jans Config Api Client" /opt/jans/jans-setup/logs/setup.log | cut -d'=' -f2 |rev | cut -d',' -f2 |rev'`
				
                                ./jans-cli-test.exp ${HOST} ${USERNAME} ${OPENIDINUM} 2>&1 >$LOG_LOCATION/jans-cli_test.log
				
				reportgen			

                                break;;
                                
			"CentOS")
	
				echo " login to CentOS Host"
				if ( ssh  ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
					then

expect<<EOF

spawn /usr/bin/ssh $USERNAME@$HOST
expect -re "(.*)"
send "python3 install.py -uninstall\r"
expect {
"python3: command not found" { send "sudo dnf install python3 -y\r" }
"Jans server seems not installed" { send "yes\r" }
-re "(.*)" { send "yes\r" }
}


sleep 10
expect "/opt/dist"
expect eof
EOF
				fi


				#2>&1 >$LOG_LOCATION/install.log
				./install.exp ${HOST} ${USERNAME} 2>&1 >$LOG_LOCATION/install.log
				sleep 20			
				OPENIDINUM=`ssh  ${USERNAME}@${HOST} 'grep -ir "Jans Config Api Client" /opt/jans/jans-setup/logs/setup.log | cut -d'=' -f2 |rev | cut -d',' -f2 |rev'`
				
                                ./jans-cli-test.exp ${HOST} ${USERNAME} ${OPENIDINUM} 2>&1 >$LOG_LOCATION/jans-cli_test.log
				
				reportgen			

                                break;;
                    			                                
                    

			Suse)
				echo "Login to Suse Host"
				
			 if ( ssh  ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
                                        then

expect<<EOF
spawn /usr/bin/ssh $USERNAME@$HOST
expect -re "(.*)"
send "python3 install.py -uninstall\r"
expect -re "(.*)"
send "yes\r"
sleep 10
expect "/opt/dist"
expect eof
EOF
                        fi


                                #2>&1 >$LOG_LOCATION/install.log
                                ./install.exp ${HOST} ${USERNAME} 2>&1 >$LOG_LOCATION/install.log
                                OPENIDINUM=`ssh  ${USERNAME}@${HOST} 'grep -ir "Jans Config Api Client" /opt/jans/jans-setup/logs/setup.log | cut -d'=' -f2 |rev | cut -d',' -f2 |rev'`
                                ./jans-cli-test.exp ${HOST} ${USERNAME} ${OPENIDINUM} 2>&1 >$LOG_LOCATION/jans-cli_test.log
				reportgen	
                                break;;


			Red)
				echo "Login to RHEL8 Host"
				
				 if ( ssh  ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
                                        then

expect<<EOF
spawn /usr/bin/ssh $USERNAME@$HOST
expect -re "(.*)"
send "python3 install.py -uninstall\r"
expect -re "(.*)"
send "yes\r"
sleep 10
expect "/opt/dist"
expect eof
EOF
                                fi


                                #2>&1 >$LOG_LOCATION/install.log
                                ./install.exp ${HOST} ${USERNAME} 2>&1 >$LOG_LOCATION/install.log
                                OPENIDINUM=`ssh  ${USERNAME}@${HOST} 'grep -ir "Jans Config Api Client" /opt/jans/jans-setup/logs/setup.log | cut -d'=' -f2 |rev | cut -d',' -f2 |rev'`
                                ./jans-cli-test.exp ${HOST} ${USERNAME} ${OPENIDINUM} 2>&1 >$LOG_LOCATION/jans-cli_test.log
				
				reportgen

                                break;;
			Rocky)
				echo "Login to Rocky Linux 8 Host"


                                ./install.exp ${HOST} ${USERNAME}
                                break;;

		esac
	
	done

