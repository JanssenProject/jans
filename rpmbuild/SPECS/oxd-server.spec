%define name1 oxd-server-4.0.beta
Name:           oxd-server-4.0.beta
Version:        1
Release:        1.centos6
Summary:        plugins for OpenID and UMA
Group:          System Environment/Daemons
License:        MIT
URL:            http://www.gluu.org
Source0:        %{name}.tar.gz
Source1:        oxd-server-4.0.beta.init.d
Source2:        oxd-server-default
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
install -m 755 %SOURCE1 %{buildroot}/etc/init.d/%{name1}
install -m 755 %SOURCE1 %{buildroot}/%{_initddir}/%{name1}
install -m 644 %SOURCE2 %{buildroot}/etc/default/oxd-server-4.0.beta
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
chkconfig --add oxd-server-4.0.beta
getent passwd jetty > /dev/null 2>&1
if [ $? -ne 0 ]; then
    /usr/sbin/useradd --system --create-home --user-group --shell /bin/bash --home-dir /home/jetty jetty
fi
getent group gluu > /dev/null 2>&1
if [ $? -ne 0 ]; then
    /usr/sbin/groupadd gluu
    /usr/sbin/usermod -a -G gluu jetty
fi

chown root:root /etc/default/oxd-server-4.0.beta 2>&1
chown jetty:jetty -R /opt/oxd-server 2>&1
chmod 644 /etc/default/oxd-server-4.0.beta 2>&1
mkdir -p /var/log/oxd-server 2>&1
touch /var/log/oxd-server/oxd-server.log 2>&1
chown -R jetty:jetty /var/log/oxd-server 2>&1
chown -R jetty:jetty /opt/oxd-server/data 2>&1
chmod 764 /opt/oxd-server/data 2>&1

%preun
if [ -x /etc/init.d/oxd-server-4.0.beta ] || [ -e /etc/init/oxd-server.conf ]; then
/etc/init.d/oxd-server-4.0.beta stop > /dev/null 2>&1
fi
chkconfig --del oxd-server-4.0.beta

%files
%defattr(-,root,root,-)
/opt/oxd-server/*
/etc/init.d/%{name1}
%{_initddir}/%{name1}
/etc/default/oxd-server-4.0.beta
/var/log/oxd-server

%changelog
* Mon Mar 07 2016 Adrian Alves <adrian@gluu.org> - 4.0.0-Beta
- Release 4.0.0-Beta
