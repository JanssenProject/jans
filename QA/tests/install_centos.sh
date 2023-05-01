
sudo sed -i 's/SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config
#yum -y update
#sudo yum install -y gcc openssl-devel libffi-devel bzip2-devel wget
#cd /opt
#wget https://www.python.org/ftp/python/3.8.9/Python-3.8.9.tgz
#tar xzvf Python-3.8.9.tgz
#cd Python-3.8.9/
#sudo ./configure --enable-optimizations
#sudo make altinstall
#
#sudo update-alternatives --install /usr/bin/python3 python3 /usr/local/bin/python3.8 1
#yum install epel-release
#dnf module enable mod_auth_openidc
#sudo yum install python3-pip

sudo rpm -Uvh https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm	
sudo /usr/bin/crb enable
dnf module enable mod_auth_openidc
sudo yum update -y
curl https://raw.githubusercontent.com/JanssenProject/jans/v1.0.7/jans-linux-setup/jans_setup/install.py > install.py


