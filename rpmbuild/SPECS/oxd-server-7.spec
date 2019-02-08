%define name1 oxd-server-4.0.beta
Name:           oxd-server-4.0.beta
Version:        1
Release:        1.centos7
Summary:        plugins for OpenID and UMA
Group:          System Environment/Daemons
License:        MIT
URL:            http://www.gluu.org
Source0:        %{name}.tar.gz
Source1:        oxd-server-4.0.beta.service
Source2:        oxd-server.sh
BuildArch:      noarch
Conflicts:      oxd-server-4.0.beta

%description
oxd - OpenID Connect and UMA plugins for
Apache and nginx web containers

%prep
%setup -qn %{name}

%build
#mvn clean package -U -Dmaven.test.skip=true


%install
rm -rf $RPM_BUILD_ROOT
install -d %{buildroot}/opt/oxd-server
install -d %{buildroot}/opt/oxd-server/bin
install -d %{buildroot}/opt/oxd-server/data
install -d %{buildroot}/opt/oxd-server/conf
install -d %{buildroot}/opt/oxd-server/lib
install -d %{buildroot}/etc/init.d
install -d %{buildroot}/%{_initddir}
install -d %{buildroot}/etc/default/
install -m 755 %SOURCE1 %{buildroot}/lib/systemd/system/oxd-server-4.0.beta.service
install -m 755 %SOURCE2 %{buildroot}/opt/oxd-server/bin/oxd-server.sh
install -m 755 oxd-server/src/main/bin/oxd-start.sh %{buildroot}/opt/oxd-server/bin/
install -m 755 oxd-server/src/main/bin/lsox.sh %{buildroot}/opt/oxd-server/bin/
install -m 644 oxd-server/src/main/resources/oxd-server.yml %{buildroot}/opt/oxd-server/conf/
install -m 644 oxd-server/src/main/resources/oxd-server.keystore %{buildroot}/opt/oxd-server/conf/
install -m 644 oxd-server/src/main/resources/swagger.yaml %{buildroot}/opt/oxd-server/conf/
install -m 644 bcprov-jdk15on-1.54.jar %{buildroot}/opt/oxd-server/lib/
install -m 644 oxd-server/target/oxd-server.jar %{buildroot}/opt/oxd-server/lib/
install -m 644 README.md %{buildroot}/opt/oxd-server/
install -m 644 license.md %{buildroot}/opt/oxd-server/
mkdir -p %{buildroot}/var/log/oxd-server
mkdir -p %{buildroot}/etc/default

%clean
rm -rf $RPM_BUILD_ROOT

%pre
# Stopping oxd-server
# This will stop oxd-server before upgrade|install 
if [ -e /var/run/oxd-server-4.0.beta.pid ]; then
    kill -9 `cat /var/run/oxd-server-4.0.beta.pid` > /dev/null 2>&1
    rm -rf /var/run/oxd-server-4.0.beta.pid > /dev/null 2>&1
fi

%post
chkconfig --add oxd-server-4.0.beta > /dev/null 2>&1
getent passwd jetty > /dev/null 2>&1
if [ $? -ne 0 ]; then
    /usr/sbin/useradd --system --create-home --user-group --shell /bin/bash --home-dir /home/jetty jetty
fi
getent group gluu > /dev/null 2>&1
if [ $? -ne 0 ]; then
    /usr/sbin/groupadd gluu
    /usr/sbin/usermod -a -G gluu jetty
fi

chown root:root /lib/systemd/system/oxd-server-4.0.beta.service 2>&1
chown jetty:jetty -R /opt/oxd-server 2>&1
chmod 755 /opt/oxd-server/bin/oxd-server.sh 2>&1
mkdir -p /var/log/oxd-server 2>&1
touch /var/log/oxd-server/oxd-server.log 2>&1
chown -R jetty:jetty /var/log/oxd-server 2>&1
chown -R jetty:jetty /opt/oxd-server/data 2>&1
chmod 764 /opt/oxd-server/data 2>&1

%preun
systemctl stop oxd-server-4.0.beta.service > /dev/null 2>&1
chkconfig --del oxd-server-4.0.beta > /dev/null 2>&1

%files
%defattr(-,root,root,-)
/opt/oxd-server/*
/var/log/oxd-server
/lib/systemd/system/oxd-server-4.0.beta.service

%changelog
* Mon Mar 07 2016 Adrian Alves <adrian@gluu.org> - 4.0.0-Beta
- Release 4.0.0-Beta
