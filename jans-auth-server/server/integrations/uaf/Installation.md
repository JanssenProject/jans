This list of steps needed to do to enable SAML person authentication module.

1. Configure apache HTTP proxy to access UAF server:  
    - Make sure that enabled next apache2 plugins: proxy, proxy_http, ssl
      To enable them use next commands:
        * a2enmod proxy
        * a2enmod proxy_http
        * a2enmod ssl
    - Add to Apache2 https_gluu.conf file next lines:
```
        SSLProxyEngine on
        SSLProxyCheckPeerCN on
        SSLProxyCheckPeerExpire on

        <Location /nnl>
                ProxyPass https://evaluation4.noknoktest.com:8443/nnl retry=5 disablereuse=On
                ProxyPassReverse https://evaluation4.noknoktest.com:8443/nnl
                Order allow,deny
                Allow from all
        </Location>
```
	Proxy between UAF and oxAuth sever is needed because UAF server not supports CORS operations. In this configuration we uses evaluation server.

2. Confire new custom module in oxTrust:
    - Log into oxTrust with administrative permissions.
    - Open "Configuration→Manage Custom Scripts" page.
    - Select "Person Authentication" tab.
    - Click on "Add custom script configuration" link.
    - Enter name = uaf
    - Enter level = 0-100 (priority of this method).
    - Select usage type "Interactive".
    - Add custom required and optional properties which specified in "Properties description.md".
    - Copy/paste script from UafExternalAuthenticator.py.
    - Activate it via "Enabled" checkbox.
    - Click "Update" button at the bottom of this page.

3. Configure oxAuth to use UAF authentication by default:
    - Log into oxTrust with administrative permissions.
    - Open "Configuration→Manage Authentication" page.
    - Scroll to "Default Authentication Method" panel. Select "uaf" authentication mode.
    - Click "Update" button at the bottom of this page.

4. Try to log in using UAF authentication method:
    - Wait 30 seconds and try to log in again. During this time oxAuth reload list of available person authentication modules.
    - Open second browser or second browsing session and try to log in again. It's better to try to do that from another browser session because we can return back to previous authentication method if something will go wrong.

There are log messages in this custom authentication script. In order to debug this module we can use command like this:
tail -f /opt/tomcat/logs/wrapper.log | grep "UAF"
