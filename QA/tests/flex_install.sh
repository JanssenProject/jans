wget https://raw.githubusercontent.com/GluuFederation/flex/main/flex-linux-setup/flex_linux_setup/flex_setup.py  -O flex_setup.py
python3 flex_setup.py -f setup.properties -n -c

cd /opt/jans/jetty/casa
touch .administrable



