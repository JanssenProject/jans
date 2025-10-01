Name:           jans
Version:        %VER%
Release:        %RELEASE%
Summary:        Janssen
License:        Apache-2.0
URL:            https://jans.io/
Source0:        jans-%VER%.tar.gz
Requires:       httpd, mod_ssl, mod_auth_openidc, curl, wget, tar, xz, unzip, rsyslog, bzip2, python3-requests, python3-ruamel-yaml, python3-certifi, python3-PyMySQL, python3-cryptography, python3-psycopg2
%description
Janssen enables organizations to build a scalable centralized authentication and authorization service using free open source software.

%global _python_bytecompile_extra 0
%global _python_bytecompile_errors_terminate_build 0
%global __os_install_post %(echo '%{__os_install_post}' | sed -e 's!/usr/lib[^[:space:]]*/brp-python-bytecompile[[:space:]].*$!!g')
%undefine __brp_python_bytecompile

%prep
%setup -q 

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p %{buildroot}/opt/dist/app
mkdir -p %{buildroot}/opt/dist/jans
mkdir -p %{buildroot}/opt/dist/scripts
mkdir -p %{buildroot}/opt/jans/jans-setup
cp -r opt/dist/* %{buildroot}/opt/dist/
cp -r opt/jans/* %{buildroot}/opt/jans/


%preun
if [ $1 == 0 ]; then
    if [ -f /etc/systemd/system/jans-auth.service ]; then
        systemctl stop jans-auth.service
    fi
    if [ -f /etc/systemd/system/jans-config-api.service ]; then
        systemctl stop jans-config-api.service
    fi
    if [ -f /etc/systemd/system/jans-fido2.service ]; then
        systemctl stop jans-fido2.service
    fi
    if [ -f /etc/systemd/system/jans-scim.service ]; then
        systemctl stop jans-scim.service
    fi

    if [ -d /opt/jans.saved ]; then
        rm -rf /opt/jans.saved
    fi
    if [ -d /opt/jans ]; then
        echo "Your changes will be saved into /opt/jans.saved"
        mkdir /opt/jans.saved
        cp -rp /opt/jans /opt/jans.saved/
        if [ -d /opt/dist ]; then
            cp -rp /opt/dist /opt/jans.saved/
        fi
        if [ -e /opt/jetty-* ]; then
            cp -rp /opt/jetty-* /opt/jans.saved/
        fi
        if [ -e /opt/jython-* ]; then
            cp -rp /opt/jython-* /opt/jans.saved/
        fi
        if [ -e /opt/opendj ]; then
            cp -rp /opt/opendj /opt/jans.saved/
        fi
        if [ -e /opt/amazon-corretto-* ]; then
            cp -rp /opt/amazon-corretto-* /opt/jans.saved/ 
        fi
    fi
fi


%postun
if [ $1 == 0 ]; then
    rm -rf /etc/systemd/system/jans-*.service    
    rm -rf /etc/systemd/system/opendj.service    
    rm -rf /etc/jans 
    rm -rf /etc/certs
    rm -rf /etc/default/jans-*
    rm -rf /opt/jans
    rm -rf /opt/dist
    rm -rf /opt/opendj
    rm -rf /opt/amazon-corretto-*
    rm -rf /opt/jython*
    rm -rf /opt/jetty*
    rm -rf /opt/jre
fi


%files
/opt/dist/*
/opt/jans/*

%changelog
* Wed Jan 26 2022 Davit Nikoghosyan <davit@gluu.org> - 1-1
- Initial release
