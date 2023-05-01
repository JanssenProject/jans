#!/usr/bin/bash -x

LOG_LOCATION=./logs
exec > >(tee -i $LOG_LOCATION/install.log)
exec 2>&1

if [[ ! -d ./logs || ! -d ./report ]]
then
  mkdir -p ./logs ./report
fi

#rm  -rf  $LOG_LOCATION/* ./report/result.txt

helpFunction()
{
   echo ""
   echo "Usage:  -h servername -u username -p passwd -b branchname"
   echo -e "\t-host name of the host server -u username of host -p passwd of user "
   exit 1 # Exit script after printing help
}

unset servername username passwd branchname
while getopts h:u:p:b: option
do
case "${option}"
in
h) HOST=${OPTARG};;
u) USERNAME=${OPTARG};;
p) PASSWD=${OPTARG};;
b) BRANCH=${OPTARG};;
esac
done

# Print helpFunction in case parameters are empty

if [ -z ${HOST} ] && [ -z ${USERNAME} ] && [ -z ${PASSWD} ] && [ -z ${BRANCH} ]
  then
	echo " host parameter is empty or username or passwd is not added"
	helpFunction
fi 
# Begin script in case all parameters are correct
echo "your host is $HOST"
echo "your username is ${USERNAME}"
echo "your passwd is ${PASSWD}"
echo "your branch is  ${BRANCH}"

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
cp TESTREPORT.md ./report/TESTREPORT-`hostname`-`date +%y%m%d`-`date +%H%M`.md
reportfile="./report/TESTREPORT-`hostname`-`date +%y%m%d`-`date +%H%M`.md"

os_name=`uname -v | awk {'print$1'} | cut -f2 -d'-'`
upt=`uptime | awk {'print$3'} | cut -f1 -d','`
ip_add=`ifconfig | grep "inet addr" | head -2 | tail -1 | awk {'print$2'} | cut -f2 -d:`
num_proc=`ps -ef | wc -l`
ram_usage=`free -m | head -2 | tail -1 | awk {'print$3'}`
ram_total=`free -m | head -2 | tail -1 | awk {'print$2'}`
inode=`df -i / | head -2 | tail -1 | awk {'print$5'}`
os_version=`uname -v | cut -f2 -d'~' | awk {'print$1'} | cut -f1 -d'-' | cut -c 1-5`

sed -e "s/osname/$os_name/g" -e "s/osversion/$os_version/g" -e "s/ipaddress/$ip_add/g" -e "s/uptime/$upt/g" ./report/TESTREPORT.md >>$reportfile

while read name status;
     do
        name1=$(grep -i $name $reportfile | cut -d"|" -f2 | rev | cut -d' ' -f2 | rev )
        echo "type $name1"
       if [ "${name}" = "${name1}" ]
         then
        status2=$(grep -i $name $reportfile | cut -d"|" -f3 |sed -e 's/^[ \t]*//' | sed -e 's/\ *$//g')
        echo "status $status2"
               sed -i s@"$name1|$status2"@"$name|$status"@ $reportfile

        fi
done <./report/result.txt
echo "Report has been generated in report with file-name = $reportfile ."
#Sending Email to the user

}

install_jans() {

rm setup.properties
IP_ADDRESS=$HOST
HOSTNAME=`ssh $USERNAME@$HOST hostname`
ORG_NAME=glu
EMAIL=manoj@gluu.org
CITY=pune
STATE=MH
COUNTRY=IN
ADMIN_PASS="Admin@123"
LDAP=True
 echo "*****   Writing properties!!   *****"
  echo "ip=${IP_ADDRESS}" |  tee -a setup.properties > /dev/null
  echo "hostname=${HOSTNAME}" |  tee -a setup.properties > /dev/null
  echo "orgName=${ORG_NAME}" |  tee -a setup.properties > /dev/null
  echo "admin_email=${EMAIL}" |  tee -a setup.properties > /dev/null
  echo "city=${CITY}" |  tee -a setup.properties > /dev/null
  echo "state=${STATE}" | tee -a setup.properties > /dev/null
  echo "countryCode=${COUNTRY}" | tee -a setup.properties > /dev/null
  echo "ldapPass=${ADMIN_PASS}" | tee -a setup.properties > /dev/null
  echo "installLdap=${LDAP}" | tee -a setup.properties > /dev/null
curl https://raw.githubusercontent.com/JanssenProject/jans/$BRANCH/jans-linux-setup/jans_setup/install.py > install.py
scp setup.properties install.py $USERNAME@$HOST:~/
rm setup.properties install.py
expect<<EOF
spawn /usr/bin/ssh $USERNAME@$HOST
expect -re "(.*)"
#send -- "sudo apt update -y\r"
#expect "password"
#send "${PASSWD}\r"
#send -- "sudo apt install -y wget curl\r"
#expect -re "(.*)\n"
send "sudo python3 install.py \-\-args=\"-f setup.properties -c -n \-\-cli-test-client\"\r"
expect {

"python3-ldap3" { send "y\r" }
"install these now" { send "y\r" }
-re "(.*)" { send "\r" }
}
sleep 620
expect "Janssen Server installation successful"
expect eof
EOF

} 

uninstall_jans(){

expect<<EOF

spawn /usr/bin/ssh $USERNAME@$HOST
expect -re "(.*)"
send "sudo python3 install.py -uninstall\r"
expect "password"
send "${PASSWD}\r"
expect {
"python3: command not found" { send "sudo dnf install python3 -y\r" }
"Jans server seems not installed" { send "yes\r" }
-re "(.*)" { send "yes\r" }
	}
sleep 10
expect "/opt/dist"
expect eof
EOF
}	

expect <<EOF
spawn ssh-keygen  -t rsa -P '' -f ~/.ssh/id_rsa
expect { "Overwrite (y/n)?" {send "y\r"} }
spawn ssh-copy-id -f -i "$HOME/.ssh/id_rsa.pub" ${USERNAME}@${HOST}
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
						uninstall_jans 
				fi
				install_jans 2>&1 >$LOG_LOCATION/install.log
				sleep 20
				
				./jans_command.sh ${USERNAME} ${HOST}  2>&1 >$LOG_LOCATION/jans-cli_test.log
				
				reportgen			
                break;;            
			"CentOS")
				echo " login to CentOS Host"
				if ( ssh  ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
					then
						uninstall_jans 
				fi
				install_jans  2>&1 >$LOG_LOCATION/install.log
				sleep 20			
				
                ./jans_command.sh ${USERNAME} ${HOST}  2>&1 >$LOG_LOCATION/jans-cli_test.log
				
				reportgen			
                break;;
							    			                                
			"openSUSE")
				echo "Login to Suse Host"
				
			 	if ( ssh  ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
                                        then
						uninstall_jans
                        fi
                install_jans  2>&1 >$LOG_LOCATION/install.log
                                
                ./jans_command.sh ${USERNAME} ${HOST}  2>&1 >$LOG_LOCATION/jans-cli_test.log
				reportgen	
                break;;


			"Red")
				echo "Login to RHEL8 Host"
				
				if ( ssh  ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
                    then
						uninstall_jans	2>&1 >$LOG_LOCATION/install.log
                fi                             
                # ./install.exp ${USERNAME} ${HOST} 2>&1 >$LOG_LOCATION/install.log
				install_jans  2>&1 >$LOG_LOCATION/install.log
                                
                ./jans_command.sh ${USERNAME} ${HOST}  2>&1 >$LOG_LOCATION/jans-cli_test.log
				reportgen
                break;;


			"AlmaLinux")
				echo "Login to AlmaLinux  Host"

            	if ( ssh  ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
                    then
                		uninstall_jans  2>&1 >$LOG_LOCATION/install.log
                fi
            install_jans  2>&1 >$LOG_LOCATION/install.log
                                
            ./jans_command.sh ${USERNAME} ${HOST}  2>&1 >$LOG_LOCATION/jans-cli_test.log
            reportgen
            break;;

		esac
	
	done
#rm -rf logs/ report/
sed -i 's/
//g' ./logs/*.log
sed -i 's/^[//g' ./logs/*.log
sed -i 's/^G[//g' ./logs/*.log

sed -i 's/^[[38;5;//g' ./logs/*.log
sed -i 's/^[]0;//g' ./logs/*.log
sed -i 's/^[[0m//g' ./logs/*.log

sed -i 's/^[[3J^[[H^[[2J//g' ./logs/*.log
