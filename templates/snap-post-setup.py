#!{{SNAP_PY3}}

import os

snap_name = "{{SNAP_NAME}}"

snap_plugs = ['mount-observe', 'system-observe', 'network-observe']

print("Connecting plugs: {}".format(', '.join(snap_plugs)))

for plug in snap_plugs:
    os.system('snap connect {0}:{1} :{1}'.format(snap_name, plug))

print("Setting ulimits")

limits_fn = '/etc/security/limits.conf'
limits = {
        'root':     {'soft': {'nofile': 131072}, 'hard': {'nofile': 262144}},
        'daemon':   {'soft': {'nofile': 131072}, 'hard': {'nofile': 262144}},
    }

limits_done = {}

for sd in limits:
    limits_done[sd] = {}
    for st in limits[sd]:
        limits_done[sd][st] = {}
        for si in limits[sd][st]:
            limits_done[sd][st][si] = False

with open(limits_fn) as f:
    limits_content = f.readlines()

def get_limits_line(sdomain, stype, sitem, new_val):
    return '{}{}{}{}\n'.format(sdomain.ljust(20), stype.ljust(8), sitem.ljust(15), new_val)

for i, l in enumerate(limits_content):
    if l and not l.startswith('#'):
        lsp = l.strip().split()
        if len(lsp) >= 4:
            sdomain = lsp[0]
            stype = lsp[1]
            sitem = lsp[2]
            svalue = lsp[3]

            new_val = limits.get(sdomain, {}).get(stype,{}).get(sitem)
            if new_val:
                limits_content[i] = get_limits_line(sdomain, stype, sitem, new_val)
                limits_done[sdomain][stype][sitem] = True

for sd in limits_done:
    for st in limits_done[sd]:
        for si in limits_done[sd][st]:
           if not limits_done[sd][st][si]:
               limits_content.append(get_limits_line(sd, st, si, limits[sd][st][si]))
            
with open(limits_fn, 'w') as w:
    w.write(''.join(limits_content))


