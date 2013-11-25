#! /usr/bin/env python

#This creates the necessary impala commands to pre-create partitions. It's output should be redirected
#to a file that can be executed by the impala-shell. For example:
# $>python preppartitions.py > create-partitions.imp
# $>impala-shell -f create-partitions.imp  


import os

db = 'cis'
tabledir = '/user/hive/warehouse/cis.db/records/'
partition = 'hour'

#full set of partitions
years = ['2013', '2014']
months = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
days = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31']
hours = ['00', '01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23']

#subset for testing
years = ['2013']
months = ['11']
days = ['19','25', '26', '27', '28', '29', '30', '31']


val = 'use ' + db + ';\n\n'
for y in years:
    for m in months:
        for d in days:
            for h in hours:
                val = val + "alter TABLE records ADD PARTITION (" + partition + " = " +y+m+d+h + ") location '" + tabledir + partition + "=" +y+m+d+h + "';" + "\n"

print val