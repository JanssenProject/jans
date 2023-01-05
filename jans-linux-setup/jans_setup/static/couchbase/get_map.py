import requests
import re
import json

result = requests.get('https://raw.githubusercontent.com/GluuFederation/oxCore/master/persistence-core/src/main/java/org/gluu/persist/key/impl/KeyShortcuter.java')

java_map = result.text

map_list = []

for l in java_map.split('\n'):
    ls = l.strip()
    if ls.startswith('put'):
        re_match = re.findall('"(\w*)"',ls)
        map_list.append(re_match)


json.dump(map_list, open('maps.json','w'),  indent=2)
