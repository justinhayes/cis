#! /usr/bin/env python

#This creates the necessary impala commands to pre-create partitions. It's output should be redirected
#to a file that can be executed by the impala-shell. For example:
# $>python preppartitions.py > create-partitions.imp
# $>impala-shell -f create-partitions.imp

#See http://www.epochconverter.com/ for conversions


import os
import calendar
import time

db = 'cis'
tabledir = '/user/hive/warehouse/cis.db/records/'
partition = 'hour'


###This approach takes the number of seconds since the epoch does rem operator on numSecondsInAnHour to get the
###number of epoch seconds for the current hour we are in. This works with ZoomData.
numpartitionstomake = 24 * 1 #hoursinday * days
secsinhour = 60 * 60
timeinsec = calendar.timegm(time.gmtime())
basehour = timeinsec / secsinhour
part = basehour * secsinhour #the start of the next hour
part = part - secsinhour #the start of the current hour
val = 'use ' + db + ';\n\n'
for i in range(numpartitionstomake):
    val = val + "alter TABLE records ADD PARTITION (" + partition + " = " + str(part) + ") location '" + tabledir + partition + "=" + str(part) + "';" + "\n"
    part = part + secsinhour #the start of the next hour

print val

###This approach uses an "hour" pattern of YYYYMMDDHH; does not work with ZoomData
##full set of partitions
#years = ['2013', '2014']
#months = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
#days = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31']
#hours = ['00', '01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23']
#
##subset for testing
#years = ['2013']
#months = ['11']
#days = ['19','25', '26', '27', '28', '29', '30', '31']
#
#val = 'use ' + db + ';\n\n'
#for y in years:
#    for m in months:
#        for d in days:
#            for h in hours:
#                val = val + "alter TABLE records ADD PARTITION (" + partition + " = " +y+m+d+h + ") location '" + tabledir + partition + "=" +y+m+d+h + "';" + "\n"
#
#print val