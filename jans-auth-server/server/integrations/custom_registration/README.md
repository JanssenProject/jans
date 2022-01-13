In order to exceute the custom registration workflow, 

1. Add the following custom property(key:value) in the script properties:

attributes_json_file_path: /etc/Attributes.json

and place Attributes.json file in the path specified in the custom property.

2. Place the reg.xhtml file in /opt/gluu/jetty/oxauth/custom/pages/auth/ directory.

3. Enable the script and set it as the default authentication method.
