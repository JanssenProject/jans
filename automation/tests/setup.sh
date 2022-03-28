#!/usr/bin/bash -x
##!/usr/bin/expect  -d


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
echo "$HOST"
echo "${USERNAME}"
echo "${PASSWD}"

#USER=(root root)


ssh-keygen  -t rsa -P '' -f ~/.ssh/id_rsa &> /dev/null
expect <<EOF
spawn ssh-copy-id -f -i ${USERNAME}@${HOST}
expect {
  "yes/no" { send "yes\n";exp_continue }
  "password" { send "${PASSWD}\n" }
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
				
				./install.exp ${HOST} ${USERNAME}
				break;;
			Suse)
				echo "Login to Suse Host"
				
				./install.exp ${HOST} ${USERNAME}
                                break;;


			Red)
				echo "Login to RHEL8 Host"
				

				./install.exp ${HOST} ${USERNAME}
                                break;;

		esac
	done
