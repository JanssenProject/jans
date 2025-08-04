#!/usr/bin/python3

import os
import argparse
from crontab import CronTab

parser = argparse.ArgumentParser(description="This script creates crontab job for clearing Janssen Server tokens")
parser.add_argument('-limit', help="Limit to delete entry per execution", type=int, default=1000)
parser.add_argument('-interval', help="Interval to execute cronjob", choices=['everyminute','hourly', 'daily', 'weekly', 'monthly', 'midnight'], default='hourly')
argsp = parser.parse_args()

cron_file = '/etc/cron.d/jans-session'
session_clean_file = '/opt/jans/data-cleaner/clean-data.py'

if not os.path.exists(cron_file):
    open(cron_file, 'w').close()

system_cron = CronTab(tabfile=cron_file, user=False)
system_cron.remove_all()
job = system_cron.new(command=f'python3 {session_clean_file} --yes -limit={argsp.limit}', user='root')
if argsp.interval != 'everyminute':
    job.setall(argsp.interval)
print(f"Writing crontab file {cron_file} with content '{job}'")
system_cron.write()


