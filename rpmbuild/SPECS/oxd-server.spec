%define name1 oxd-server-3.1.4
Name:           oxd-server-3.1.4
Version:        1
Release:        1.centos6
Summary:        plugins for OpenID and UMA
Group:          System Environment/Daemons
License:        MIT
URL:            http://www.gluu.org
Source0:        %{name}.tar.gz
Source1:        oxd-server-3.1.4.init.d
Source2:        oxd-https-extension
Source3:        oxd-server-default
BuildArch:      noarch
Conflicts:      oxd-server-3.1.4

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
install -d %{buildroot}/opt/oxd-server/conf
install -d %{buildroot}/opt/oxd-server/bin
install -d %{buildroot}/opt/oxd-server/lib
install -d %{buildroot}/etc/init.d
install -d %{buildroot}/%{_initddir}
install -d %{buildroot}/opt/oxd-https-extension/lib/
install -d %{buildroot}/etc/default/
install -m 755 %SOURCE1 %{buildroot}/etc/init.d/%{name1}
install -m 755 %SOURCE2 %{buildroot}/etc/init.d/oxd-https-extension
install -m 755 %SOURCE1 %{buildroot}/%{_initddir}/%{name1}
install -m 755 %SOURCE2 %{buildroot}/%{_initddir}/oxd-https-extension
install -m 644 %SOURCE3 %{buildroot}/etc/default/oxd-server-3.1.4
install -m 755 oxd-server/src/main/bin/oxd-start.sh %{buildroot}/opt/oxd-server/bin
install -m 755 oxd-server/src/main/bin/lsox.sh %{buildroot}/opt/oxd-server/bin
install -m 644 oxd-server/src/main/resources/log4j.xml %{buildroot}/opt/oxd-server/conf/
install -m 644 oxd-server/src/main/resources/oxd-conf.json %{buildroot}/opt/oxd-server/conf/
install -m 644 oxd-server/src/main/resources/oxd-default-site-config.json %{buildroot}/opt/oxd-server/conf/
install -m 644 oxd-server/target/oxd-server-jar-with-dependencies.jar %{buildroot}/opt/oxd-server/lib/
install -m 644 oxd-https-extension/target/oxd-https-extension-3.1.4.Final.jar %{buildroot}/opt/oxd-https-extension/lib/
install -m 644 oxd-https-extension/oxd-https.keystore %{buildroot}/opt/oxd-https-extension/lib/
install -m 644 oxd-https-extension/oxd-https.yml %{buildroot}/opt/oxd-https-extension/lib/
install -m 644 README.md %{buildroot}/opt/oxd-server/
install -m 644 LICENSE %{buildroot}/opt/oxd-server/
install -m 644 bcprov-jdk15on-1.54.jar %{buildroot}/opt/oxd-server/lib/
mkdir -p %{buildroot}/var/log/oxd-server
mkdir -p %{buildroot}/etc/oxd/oxd-server
mkdir -p %{buildroot}/etc/default/
cp -a %{buildroot}/opt/oxd-server-3.1.4/conf/* %{buildroot}/etc/oxd/oxd-server/.
cp -a debian/oxd-server-default %{buildroot}/etc/default/oxd-server-3.1.4

%clean
rm -rf $RPM_BUILD_ROOT

%post
chkconfig --add oxd-server-3.1.4
getent passwd jetty > /dev/null 2>&1
if [ $? -ne 0 ]; then
    /usr/sbin/useradd --system --create-home --user-group --shell /bin/bash --home-dir /home/jetty jetty
fi
getent group gluu > /dev/null 2>&1
if [ $? -ne 0 ]; then
    /usr/sbin/groupadd gluu
    /usr/sbin/usermod -a -G gluu jetty
fi

chown root:root /etc/default/oxd-server-3.1.4 2>&1
chown jetty:jetty -R /opt/oxd-server 2>&1
chmod 644 /etc/default/oxd-server-3.1.4 2>&1
mkdir -p /var/log/oxd-server 2>&1
touch /var/log/oxd-server/oxd-server.log 2>&1
chown -R jetty:jetty /var/log/oxd-server 2>&1

%preun
if [ -x “/etc/init.d/oxd-server-3.1.4” ] || [ -e “/etc/init/oxd-server.conf” ]; then
service oxd-server-3.1.4 stop || exit $?
fi
chkconfig --del oxd-server-3.1.4

%files
%defattr(-,root,root,-)
/opt/oxd-server/*
/opt/oxd-https-extension/*
/etc/init.d/%{name1}
/etc/init.d/oxd-https-extension
%{_initddir}/%{name1}
%{_initddir}/oxd-https-extension
/etc/default/oxd-server-3.1.4
/etc/oxd/*
/var/log/oxd-server

%changelog
* Mon Mar 07 2016 Adrian Alves <adrian@gluu.org> - 3.1.4-1
- Release 3.1.4
