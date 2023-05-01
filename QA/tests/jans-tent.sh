#!/usr/bin/bash

helpFunction() {
    echo "please run it on localhost only"
    echo "Usage: ./jans-tent.sh -p "path of jans-tent folder" -h HOSTNAME"
    echo -e "EX: ./jans-tent.sh  /home/manoj/demos/jans-tent    manojs1978-pleasing-goldfish.gluu.info  "
    exit 1 # Exit script after printing help
}

unset JANS_PATH HOSTNAME
while getopts p:h: option; do
    case "${option}" in
    p) JANS_PATH=${OPTARG} ;;
    h) HOSTNAME=${OPTARG} ;;
    esac
done
JANS_PATH=$1
HOSTNAME=$2

if [ ! -z ${JANS_PATH} ] && [ ! -z ${HOSTNAME} ]; 
then
    echo "path and hostname defined"
   cd ${JANS_PATH}
    echo $PWD

echo ${JANS_PATH}
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -sha256 -days 365 -nodes

export OP_HOSTNAME=${HOSTNAME} 

echo -ne '\n'  | openssl s_client -servername $OP_HOSTNAME -connect $OP_HOSTNAME:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' >${JANS_PATH}/op_web_cert.cer

    export CERT_PATH=$(python3 -m certifi)
    export SSL_CERT_FILE=${CERT_PATH}
    export REQUESTS_CA_BUNDLE=${CERT_PATH} && mv ${JANS_PATH}/op_web_cert.cer $CERT_PATH

    rm ${JANS_PATH}/op_web_cert ${JANS_PATH}/client_info.json
    #sed -i "s/ISSUER =*/ISSUER =/" ${JANS_PATH}/clientapp/config.py
    sed -i "s/.*ISSUER.*/ISSUER = 'https\:\/\/${OP_HOSTNAME}'/" ${JANS_PATH}/clientapp/config.py
    python ${JANS_PATH}/clientapp/register_new_client.py
    echo "simple passwd test started"
    python main.py &
    echo "press enter after completting simple auth passwd test"
    read
    PID=`ps -eaf | grep main.py | grep -v grep | awk '{print $2}'`
    if [[ "" !=  "$PID" ]]; then
    echo "killing $PID"
    kill -9 $PID
    python main.py &
     echo "press enter after completting simple auth passwd test"
    read
fi
else
     echo " some parameter are empty please check below instructions"
    helpFunction
 
fi
