import sys
import json
import os

if len(sys.argv) < 2:
    print "Usage: python loga.py <log_dir>"
    sys.exit()

log_dir = sys.argv[1]

def http_log():

    print "\nHTTP REQUEST LOG ANALYSES"
    print "="*100

    rdict = {}

    for l in open(os.path.join(log_dir, 'http_request_response.log')):
        ls = l.strip().split(' - ') 
        data = json.loads(ls[-1])
        if data.get('method') == 'GET':
            if data.get('duration'):
                d = float(data['duration'][2:-1])
                if data['path'] in rdict:
                    rdict[data['path']].append(d)
                else:
                    rdict[data['path']] = [d]

    sn = 0
    st = 0

    print 'Count\t  t_sum\t  t_avg\tPATH'

    for path in rdict:
        data = rdict[path]
        n = len(data)
        ssn = str(n).rjust(5)
        sn += n
        t = sum(data)
        st += t
        ts = '{:0.3f}'.format(t).rjust(7)
        a= t/n
        avs = '{:0.3f}'.format(a).rjust(7)
        print '{}\t{}\t{}\t{}'.format(ssn,ts,avs, path)

    print '-'*100

    sts = '{:0.3f}'.format(st).rjust(7)
    sa = st / sn
    savs = '{:0.3f}'.format(sa).rjust(7)
    sssn = str(sn).rjust(5)

    print '{}\t{}\t{}\t GRAND TOTAL'.format(sssn,sts,savs)


def durations():

    print "\nDURATIONS LOG ANALYSES"
    print "="*100


    rdict = {}


    for l in open(os.path.join(log_dir,'oxauth_persistence_duration.log')):
        ls = l.split(',')
        d = float(ls[2].strip()[12:-1])

        if len(ls)>4:

            p = ls[4].strip()
        else:
            p = ls[3].strip()
        
        if p in rdict:
            rdict[p].append(d)
        else:
            rdict[p] = [d]

    sn = 0
    st = 0

    print 'Count\t  t_sum\t  t_avg\t  Expression'

    result = []

    for path in rdict:
        data = rdict[path]
        n = len(data)
        
        sn += n
        t = sum(data)
        a= t/n
        result.append((a, n, t, path))

        st += t
        


    result.sort()
    result.reverse()

    for a, n, t, path in result:
        
        ssn = str(n).rjust(5)
        ts = '{:0.3f}'.format(t).rjust(7)
        avs = '{:0.3f}'.format(a).rjust(7)
        print '{}\t{}\t{}\t  {}'.format(ssn,ts,avs, path)

    print '-'*100

    sts = '{:0.3f}'.format(st).rjust(7)
    sa = st / sn
    savs = '{:0.3f}'.format(sa).rjust(7)
    sssn = str(sn).rjust(5)

    print '{}\t{}\t{}\t GRAND TOTAL'.format(sssn,sts,savs)

durations()
http_log()
