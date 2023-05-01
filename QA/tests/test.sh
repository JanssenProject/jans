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
