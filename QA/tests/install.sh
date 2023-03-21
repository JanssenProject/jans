#!/usr/bin/bash -x

# LOG_LOCATION=./logs
# exec > >(tee -i $LOG_LOCATION/install.log)
# exec 2>&1

# if [[ ! -d ./logs || ! -d ./report ]]
# then
#   mkdir -p ./logs ./report
# fi

#rm  -rf  $LOG_LOCATION/* ./report/result.txt



install_jans() {

sudo ssh -i ~/private.pem ${USERNAME}@${IPADDRESS} << EOF
sudo python3 install.py -uninstall -y
exit
EOF

rm setup.properties
# IP_ADDRESS=$HOST
# HOSTNAME=`sudo ssh -i ~/private.pem ${USERNAME}@${IPADDRESS} hostname`
 
# echo "$USERNAME"
# echo "$HOST"
# echo "$HOSTNAME"
ORG_NAME=test
EMAIL=test@test.org
CITY=pune
STATE=MH
COUNTRY=IN
ADMIN_PASS="Admin@123"
LDAP=True
 echo "*****   Writing properties!!   *****"
  echo "ip=${IPADDRESS}" |  tee -a setup.properties > /dev/null
  echo "hostname=${HOSTNAME}" |  tee -a setup.properties > /dev/null
  echo "orgName=${ORG_NAME}" |  tee -a setup.properties > /dev/null
  echo "admin_email=${EMAIL}" |  tee -a setup.properties > /dev/null
  echo "city=${CITY}" |  tee -a setup.properties > /dev/null
  echo "state=${STATE}" | tee -a setup.properties > /dev/null
  echo "countryCode=${COUNTRY}" | tee -a setup.properties > /dev/null
  echo "ldapPass=${ADMIN_PASS}" | tee -a setup.properties > /dev/null


if [[ $DB == opendj ]]
	then
  		echo "installLdap=${LDAP}" | tee -a setup.properties > /dev/null
	elif [[ $DB == mysql ]]
		then
		echo "rdbm_install=${LDAP}" | tee -a setup.properties > /dev/null
		echo "rdbm_install_type=1" | tee -a setup.properties > /dev/null
	elif [[ $DB == pgsql ]]
		then
		echo "rdbm_install=${LDAP}" | tee -a setup.properties > /dev/null
                echo "rdbm_install_type=1" | tee -a setup.properties > /dev/null
		echo "rdbm_type=pgsql" | tee -a setup.properties > /dev/null

	elif [[ $DB == couchbase ]]
		then
		echo "rdbm_install=False" | tee -a setup.properties > /dev/null
		echo "rdbm_install_type=0" | tee -a setup.properties > /dev/null
                echo "rdbm_type=mysql" | tee -a setup.properties > /dev/null
		echo "cb_install=1" | tee -a setup.properties > /dev/null
		echo "cb_password=Admin@123" | tee -a setup.properties > /dev/null
		echo "persistence_type=couchbase" | tee -a setup.properties > /dev/null
	else
		echo "please select any DB"
fi
		
		curl https://raw.githubusercontent.com/JanssenProject/jans/v${BRANCH}/jans-linux-setup/jans_setup/install.py > install.py
		echo "install downloaded"
		sudo scp  -i ~/private.pem  setup.properties install.py ${USERNAME}@${IPADDRESS}:~/
		rm setup.properties install.py
		sudo ssh -i ~/private.pem ${USERNAME}@${IPADDRESS} << EOF
if [[ $OS == ubuntu ]];
    then
 		sudo apt install  python3-pip -y
	elif [[ $OS == rhel ]] || [[ $OS == centos ]];
    then
			sudo sed -i 's/SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config
			sudo rpm -Uvh https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm

			sudo /usr/bin/crb enable
	 		sudo dnf module enable mod_auth_openidc
			sudo yum update -y
			sudo yum install python3.8 -y
			sudo yum install  python3-pip -y
	elif [[ $OS == suse ]];
	then
			sudo zypper install python3-pip
			sudo pip3 install python3-PyMySQL PyMySQL ruamel.yaml
	else
		echo "db not selected"
fi
	if [[ $OS == rhel ]] || [[ $OS == centos ]] || [[ $OS == ubuntu ]] || [[ $OS == suse ]] && [[ $DB == couchbase ]] ;
	then
                        sudo mkdir -p /opt/dist/couchbase
                        cd /opt/dist/couchbase
						echo "***********************************************************$PWD"
			if [[ $OS == rhel ]] || [[ $OS == centos ]]
			then
 			sudo wget https://packages.couchbase.com/releases/7.1.1/couchbase-server-enterprise-7.1.1-rhel8.x86_64.rpm
			cd ~
			elif [[ $OS == ubuntu ]]
			then 
			echo "$PWD"
			sudo wget https://packages.couchbase.com/releases/7.1.1/couchbase-server-enterprise_7.1.1-ubuntu20.04_amd64.deb
			cd ~
			elif [[ $OS == suse ]]
			then 
			sudo wget https://packages.couchbase.com/releases/7.1.1/couchbase-server-enterprise-7.1.1-suse15.x86_64.rpm
			else
			echo " no couchbase DB rpm found "
			fi
	else
			echo " OS not found"
fi
exit
EOF
sudo ssh -i ~/private.pem ${USERNAME}@${IPADDRESS} << EOF
	echo " installation started"
	sleep 20
	if [[ ${FLEX_OR_JANS} == "jans" ]];
	then 
	sudo python3 install.py -y --args="-f setup.properties -c -n --cli-test-client"
	echo " installation ended"
	elif [[ ${FLEX_OR_JANS} == "flex" ]];
	then
		wget https://raw.githubusercontent.com/GluuFederation/flex/main/flex-linux-setup/flex_linux_setup/flex_setup.py  -O flex_setup.py
		python3 flex_setup.py -f setup.properties -n -c
		cd /opt/jans/jetty/casa
		touch .administrable

	else
		echo " please enter which product you want to install"
	
	fi


exit
EOF

echo "i am here"
}	


helpFunction()
{
   echo ""
   echo "Usage:  -i ipaddress -h hostname -u username -d db -o os -f flex or jans -b branch"
   echo -e "\t -i ipaddress -h hostname -u username -d db -o os -f flex or jans  -b branch"
   exit 1 # Exit script after printing help
}

unset IPADDRESS HOSTNAME USERNAME DB OS FLEX_OR_JANS
while getopts i:h:u:d:o:f:b: option
do
case "${option}"
in
i) IPADDRESS=${OPTARG};;
h) HOSTNAME=${OPTARG};;
u) USERNAME=${OPTARG};;
d) DB=${OPTARG};;
o) OS=${OPTARG};;
f) FLEX_OR_JANS=${OPTARG};;
b) BRANCH=${BRANCH};;

esac
done
#BRANCH=main
# Print helpFunction in case parameters are empty
# sudo ssh -i ~/private.pem $USERNAME@$HOST

 IPADDRESS=$1
 HOSTNAME=$2
 USERNAME=$3
 DB=$4
 OS=$5
 FLEX_OR_JANS=$6
 BRANCH=$7
# Begin script in case all parameters are correct
echo "your ip address is ${IPADDRESS}"
echo "your hostname is ${HOSTNAME}"
echo "your username is ${USERNAME}"
echo "your DB is ${DB}"
echo "your  OS is  ${OS}"
echo "you want to install  ${FLEX_OR_JANS}"
echo "your BRANCH is ${BRANCH}"

if [ -z ${IPADDRESS} ] && [ -z ${HOSTNAME} ] && [ -z ${USERNAME} ] && [ -z ${DB} ] && [ -z ${OS} ] && [ -z ${FLEX_OR_JANS} ] && [ -z ${BRANCH} ]
  then
	echo " host parameter is empty or username or passwd is not added"
	helpFunction
	else
	install_jans
fi

