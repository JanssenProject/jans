import sys
import json
import os

import argparse
parser = argparse.ArgumentParser()
parser.add_argument("--hide_key", help="Hide key string",  action="store_true")

parser.add_argument("--sort",  choices=['count', 't_sum','t_avg','expression','path'], help="Sort criteria")
parser.add_argument("--min",  type=float, default=0)

parser.add_argument("dir", help="Path to log dir")

args = parser.parse_args()

if not args.dir:
    args.print_help()
    sys.exit()


def sort_result(result):
    sort_index = 0
    
    if args.sort == 'count':
        sort_index = 1
    elif args.sort == 't_sum': 
        sort_index = 2
    elif args.sort in ('expression','path'): 
        sort_index = 3

    result.sort(key=lambda tup: tup[sort_index])
    
    result.reverse()


def print_result(result, k, heading):
        
    sort_result(result)
    
    title = ' count\t     t_sum\t     t_avg\t'
    t_un = 35

    if not args.hide_key:
        title += k
        t_un = 100
    
    print
    print heading
    print "="*t_un
    print title

    tn = 0
    ts = 0
    ta = 0
    for a, n, t, k in result:
        tn += n
        ts += t
        ta +=a
        tss = '{:0.3f}'.format(t).rjust(10)
        
        avs = '{:0.3f}'.format(a).rjust(10)
        ns = str(n).rjust(6)
        print '{}\t{}\t{}'.format(ns,tss,avs),
        if not args.hide_key:
            print "\t{}".format(k),
        print

    print '-'*t_un

    sts = '{:0.3f}'.format(ts).rjust(10)
    sa = ts / tn
    savs = '{:0.3f}'.format(sa).rjust(10)
    
    tns = str(tn).rjust(6)


    print '{}\t{}\t{}'.format(tns,sts,savs),
    
    if not args.hide_key:
        print '\tGRAND TOTAL',
    print
        
    print



def http_log():

    fn = os.path.join(args.dir, 'http_request_response.log')
    if not os.path.exists(fn):
        print "File {0} does not exists".format(fn)
        return

    rdict = {}

    for l in open(fn):
        ls = l.strip().split(' - ') 
        data = json.loads(ls[-1])
        if data.get('method') == 'GET':
            if data.get('duration'):
                d = float(data['duration'][2:-1])
                if d > args.min:
                    if data['path'] in rdict:
                        rdict[data['path']].append(d)
                    else:
                        rdict[data['path']] = [d]

    if not rdict:
        print "\n *** NO HTTP LOG ANALYSES IS AVAILABLE ***"
        return

    sn = 0
    st = 0

    result=[]

    for path in rdict:
        data = rdict[path]
        n = len(data)
        ssn = str(n).rjust(5)
        sn += n
        t = sum(data)
        st += t
        a= t/n
        
        result.append((a, n, t, path))

    print_result(result, 'path', "HTTP REQUEST LOG ANALYSES")


def durations():

    fn = os.path.join(args.dir,'oxauth_persistence_duration.log')
    if not os.path.exists(fn):
        print "File {0} does not exists".format(fn)
        return

    rdict = {}

    for l in open(fn):
        ls = l.split(',')
        d = float(ls[2].strip()[12:-1])

        if d > args.min:
            if len(ls)>6:
                p = ls[5].strip()
            else:
                p = ls[4].strip()
     
            if p in rdict:
                rdict[p].append(d)
            else:
                rdict[p] = [d]

    sn = 0
    st = 0


    result = []

    for path in rdict:
        data = rdict[path]
        n = len(data)
        
        sn += n
        t = sum(data)
        a= t/n
        result.append((a, n, t, path))

        st += t

    print_result(result, 'expression', "DURATIONS LOG ANALYSES")

durations()
http_log()
