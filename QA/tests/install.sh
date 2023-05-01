#!/usr/bin/bash

# LOG_LOCATION=./logs
# exec > >(tee -i $LOG_LOCATION/install.log)
# exec 2>&1

# if [[ ! -d ./logs || ! -d ./report ]]
# then
#   mkdir -p ./logs ./report
# fi

#rm  -rf  $LOG_LOCATION/* ./report/result.txt

uninstall_jans() {

	echo "checking  jans server installed"

	sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} <<EOF
sudo ls /opt/jans/jans-cli/config-cli.py

if [ $? -eq 0 ];
then 
echo "jans server is already installed"

if [[ $OS == "ubuntu20" ]] || [[ $OS == "ubuntu20" ]]
then
	sudo apt remove -y jans
	sudo python3 install.py -uninstall -y
	sudo apt-get remove --purge -y mysql*;sudo apt-get purge -y  mysql*;sudo apt-get -y autoremove;
	sudo apt-get -y autoclean;sudo apt-get remove -y dbconfig-mysql;sudo rm -r -f /var/lib/mysql
	sudo apt-get --purge remove -y postgresql postgresql-doc postgresql-common
	sudo mv -f /var/lib/pgsql /var/lib/pgsql_old_$$
	sudo apt remove opendj-server
	sudo /opt/opendj/uninstall
	sudo rm -rf  /opt/opendj/lib
	
elif [[ $OS == "suse" ]]
then
	sudo zypper remove -y jans
	sudo python3 install.py  -uninstall -y
	sudo zypper remove -y mysql mysql-server 
	sudo mv -f /var/lib/mysql /var/lib/mysql_old_$$
	sudo zypper remove -y postgresql postgresql-contrib postgresql-server
	sudo mv -f /var/lib/pgsql /var/lib/pgsql_old_$$
	sudo zypper remove opendj-server
	sudo /opt/opendj/uninstall
	sudo rm -rf  /opt/opendj/lib
elif [[ $OS == "rhel" ]]
then
	sudo yum remove -y jans
	sudo python3 install.py -uninstall -y
	sudo yum remove -y mysql mysql-server 
	sudo mv -f /var/lib/mysql /var/lib/mysql_old_$$
	sudo yum  remove -y  postgresql postgresql-doc postgresql-common  postgresql-contrib postgresql-server
	sudo mv -f /var/lib/pgsql /var/lib/pgsql_old_$$
	sudo yum remove opendj-server
	sudo /opt/opendj/uninstall
	sudo rm -rf  /opt/opendj/lib
fi
else
echo "jans server is not installed"
fi
exit
EOF

}

install_jans() {

	rm setup.properties
	# IP_ADDRESS=$HOST
	# HOSTNAME=`sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} hostname`

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
	echo "ip=${IPADDRESS}" | tee -a setup.properties >/dev/null
	echo "hostname=${HOSTNAME}" | tee -a setup.properties >/dev/null
	echo "orgName=${ORG_NAME}" | tee -a setup.properties >/dev/null
	echo "admin_email=${EMAIL}" | tee -a setup.properties >/dev/null
	echo "city=${CITY}" | tee -a setup.properties >/dev/null
	echo "state=${STATE}" | tee -a setup.properties >/dev/null
	echo "countryCode=${COUNTRY}" | tee -a setup.properties >/dev/null
	echo "ldapPass=${ADMIN_PASS}" | tee -a setup.properties >/dev/null

	if [[ $DB == opendj ]]; then
		echo "installLdap=${LDAP}" | tee -a setup.properties >/dev/null
	elif [[ $DB == mysql ]]; then
		echo "rdbm_install=${LDAP}" | tee -a setup.properties >/dev/null
		echo "rdbm_install_type=1" | tee -a setup.properties >/dev/null
	elif [[ $DB == pgsql ]]; then
		echo "rdbm_install=${LDAP}" | tee -a setup.properties >/dev/null
		echo "rdbm_install_type=1" | tee -a setup.properties >/dev/null
		echo "rdbm_type=pgsql" | tee -a setup.properties >/dev/null

	elif [[ $DB == couchbase ]]; then
		echo "rdbm_install=False" | tee -a setup.properties >/dev/null
		echo "rdbm_install_type=0" | tee -a setup.properties >/dev/null
		echo "rdbm_type=mysql" | tee -a setup.properties >/dev/null
		echo "cb_install=1" | tee -a setup.properties >/dev/null
		echo "cb_password=Admin@123" | tee -a setup.properties >/dev/null
		echo "persistence_type=couchbase" | tee -a setup.properties >/dev/null
	else
		echo "please select any DB"
	fi

	curl https://raw.githubusercontent.com/JanssenProject/jans/v${VERSION}/jans-linux-setup/jans_setup/install.py >install.py
	echo "install downloaded"
	sudo scp -i private.pem setup.properties install.py ${USERNAME}@${IPADDRESS}:~/
	rm setup.properties install.py

	if [[ $OS == ubuntu22 ]] || [[ $OS == ubuntu20 ]]; then
		sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} <<EOF
 		sudo apt install  python3-pip -y
		echo "package download started"
		wget https://github.com/JanssenProject/jans/releases/download/v${VERSION}/jans_${VERSION}.${OS}.04_amd64.deb &>/dev/null
		echo "package downloaded"
EOF
	elif [[ $OS == rhel ]] || [[ $OS == centos ]]; then
		sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} <<EOF
			sudo sed -i 's/SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config
			sudo yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
			sudo yum -y module enable mod_auth_openidc 
			sudo yum update -y
			sudo yum install  python3-pip -y
			sudo yum  install -y wget python3-certifi python3-ldap3 python3-prompt-toolkit python3-ruamel-yaml
			wget https://github.com/JanssenProject/jans/releases/download/v${VERSION}/jans-${VERSION}-el8.x86_64.rpm &>/dev/null
			
			sudo reboot
			
			
EOF
	elif [[ $OS == suse ]]; then
		sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} <<EOF
			echo "downloading package"
			wget https://github.com/JanssenProject/jans/releases/download/v${VERSION}/jans-${VERSION}-suse15.x86_64.rpm &>/dev/null
			sudo zypper addrepo https://download.opensuse.org/repositories/home:frispete:python/15.4/home:frispete:python.repo
			sudo zypper --non-interactive --gpg-auto-import-keys refresh
			sudo zypper --non-interactive --gpg-auto-import-keys install python3-PyMySQL
EOF
	else
		echo "db not selected"
	fi

	# 	if [[ $OS == rhel ]] || [[ $OS == centos ]] || [[ $OS == ubuntu ]] || [[ $OS == suse ]] && [[ $DB == couchbase ]] ;
	# 	then
	#                         sudo mkdir -p /opt/dist/couchbase
	#                         cd /opt/dist/couchbase
	# 						echo "***********************************************************$PWD"
	# 			if [[ $OS == rhel ]] || [[ $OS == centos ]]
	# 			then
	#  			sudo wget https://packages.couchbase.com/releases/7.1.1/couchbase-server-enterprise-7.1.1-rhel8.x86_64.rpm
	# 			cd ~
	# 			elif [[ $OS == ubuntu ]]
	# 			then
	# 			echo "$PWD"
	# 			sudo wget https://packages.couchbase.com/releases/7.1.1/couchbase-server-enterprise_7.1.1-ubuntu20.04_amd64.deb
	# 			cd ~
	# 			elif [[ $OS == suse ]]
	# 			then
	# 			sudo wget https://packages.couchbase.com/releases/7.1.1/couchbase-server-enterprise-7.1.1-suse15.x86_64.rpm
	# 			else
	# 			echo " no couchbase DB rpm found "
	# 			fi
	# 	else
	# 			echo " OS not found"
	# fi
sleep 300
	echo " installation started"
	if [[ ${PACKAGE_OR_ONLINE} == "online" ]]; then
		sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} <<EOF
	sudo python3 install.py -y --args="-f setup.properties -c -n" 
EOF
		echo " installation ended"
	elif [[ ${PACKAGE_OR_ONLINE} == "package" ]]; then
		echo "package installation started"
		if [[ ${OS} == "suse" ]]; then
			echo "${OS} package installation started"
			sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} <<EOF
			sudo zypper --no-gpg-checks install -y ./jans-${VERSION}-suse15.x86_64.rpm
			sudo python3 /opt/jans/jans-setup/setup.py -f setup.properties -n
EOF
		fi
		if [[ ${OS} == "rhel" ]]; then
			echo "${OS} package installation started"
			sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} <<EOF
			sudo yum install -y ./jans-${VERSION}-el8.x86_64.rpm
			sudo python3 /opt/jans/jans-setup/setup.py -f setup.properties -n
EOF
		fi
		if [[ ${OS} == "ubuntu20" ]] || [[ ${OS} == "ubuntu22" ]]; then
			echo "${OS} package installation started"
			sudo ssh -i private.pem ${USERNAME}@${IPADDRESS} <<EOF
			sudo apt install -y ./jans_${VERSION}.${OS}.04_amd64.deb
			sudo python3 /opt/jans/jans-setup/setup.py -f setup.properties -n
EOF
		fi
	else
		echo " please enter which product you want to install"

	fi

}

helpFunction() {
	echo "KEEP PRIVATE.PEM and INSTALL.SH IN SAME FOLDER"
	echo "Usage: ./install.sh -i IPADDRESS -h HOSTNAME -u USERNAME -d DB -o OS  -b VERSION -p PACKAGE_OR_ONLINE"
	echo -e "EX: ./install.sh  3.10.10.22  manojs1978-pleasing-goldfish.gluu.info  ec2-user  ldap  ubuntu22 OR suse OR rhel OR ubuntu20  1.0.12 OR 1.0.13.nightly  package"
	exit 1 # Exit script after printing help
}

unset IPADDRESS HOSTNAME USERNAME DB OS PACKAGE_OR_ONLINE
while getopts i:h:u:d:o:f:b:p: option; do
	case "${option}" in
	i) IPADDRESS=${OPTARG} ;;
	h) HOSTNAME=${OPTARG} ;;
	u) USERNAME=${OPTARG} ;;
	d) DB=${OPTARG} ;;
	o) OS=${OPTARG} ;;
	b) VERSION=${VERSION} ;;
	p) PACKAGE_OR_ONLINE=${PACKAGE_OR_ONLINE} ;;
	esac
done

IPADDRESS=$1
HOSTNAME=$2
USERNAME=$3
DB=$4
OS=$5
VERSION=$6
PACKAGE_OR_ONLINE=$7

echo "Begin script in case all parameters are correct"
echo "your ip address is ${IPADDRESS}"
echo "your hostname is ${HOSTNAME}"
echo "your username is ${USERNAME}"
echo "your DB is ${DB}"
echo "your  OS is  ${OS}"
echo "your VERSION is ${VERSION}"
echo "installation type is ${PACKAGE_OR_ONLINE}"

if [ -z ${IPADDRESS} ] && [ -z ${HOSTNAME} ] && [ -z ${USERNAME} ] && [ -z ${DB} ] && [ -z ${OS} ] && [ -z ${VERSION} ] && [ -z ${PACKAGE_OR_ONLINE} ]; then
	echo " some parameter are empty please check below instructions"
	helpFunction
else
	uninstall_jans
	install_jans
fi
