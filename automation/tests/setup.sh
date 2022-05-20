#!/usr/bin/bash -x
##!/usr/bin/expect  -d

mkdir logs
LOG_LOCATION=./logs
exec > >(tee -i $LOG_LOCATION/install.log)
exec 2>&1

helpFunction()
{
   echo ""
   echo "Usage:  -h servername -u username -p passwd"
   echo -e "\t-host name of the host server -u username of host -p passwd of user "
   exit 1 # Exit script after printing help
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
spawn ssh-copy-id -f -i ${USERNAME}@${HOST}
expect {
#  "yes/no" { send "yes\n";exp_continue }
  "password:" { send "${PASSWD}\r" }
}
expect eof
EOF

HOST_OS=`ssh ${USERNAME}@${HOST} 'cat /etc/os-release | grep PRETTY_NAME | cut -d" " -f-1,2 | cut -d"\"" -f2,3'` 
echo $HOST_OS
OSTYPE=`echo $HOST_OS |cut -d" " -f1` 
echo $OSTYPE

OSVERSION=`echo $HOST_OS |cut -d" " -f2` 

echo $OSVERSION


	while true

       	do
		case "$OSTYPE"  
		in
			Ubuntu)
	
				echo " login to Ubuntu Host"
				if ( ssh ${USERNAME}@${HOST} 'ls /opt/jans/jans-cli/config-cli.py' )
					then

expect <<EOF

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
				./install.exp ${HOST} ${USERNAME}
				OPENIDINUM=`ssh ${USERNAME}@${HOST} 'grep -ir "Jans Config Api Client" /opt/jans/jans-setup/logs/setup.log | cut -d'=' -f2 |rev | cut -d',' -f2 |rev'`
                                ./jans-cli-test.exp ${HOST} ${USERNAME} ${OPENIDINUM} 2>&1 >$LOG_LOCATION/jans-cli_test.log

                                break;;


			Suse)
				echo "Login to Suse Host"
				
				./install.exp ${HOST} ${USERNAME} 2>&1 >$LOG_LOCATION/install.log
			OPENIDINUM=`ssh ${USERNAME}@${HOST} 'grep -ir "Jans Config Api Client" /opt/jans/jans-setup/logs/setup.log | cut -d'=' -f2 |rev | cut -d',' -f2 |rev'`
				./jans-cli-test.exp ${HOST} ${USERNAME} ${OPENIDINUM} 2>&1 >$LOG_LOCATION/jans-cli_test.log
				
                                break;;


			Red)
				echo "Login to RHEL8 Host"
				

				./install.exp ${HOST} ${USERNAME} 
                                break;;
			Rocky)
				echo "Login to Rocky Linux 8 Host"


                                ./install.exp ${HOST} ${USERNAME}
                                break;;

		esac
	
	done
