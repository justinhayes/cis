#! /usr/bin/env python

import os
import sys
import time
import urllib2
from optparse import OptionParser
from random import choice
from string import Template

################################
#some global vars

totalbyteswritten = 0
currbyteswritten = 0
fileswritten = 0
totalrequests = 0
writespersleep = 1000
tmplogdir = '/tmp/'


################################
#parse options

parser = OptionParser()
parser.add_option('-t', '--throttle', dest='throttle', default=100, type='float', help='How much to throttle testing by; interpreted as number of ms to sleep after each oryx serving call.')
parser.add_option('-y', '--type', dest='type', default='httpd', help='Type of log data to test: "httpd", "iptables", "kdd".')
parser.add_option('-d', '--dir', dest='dir', help='Directory to read the log file data from; defaults to the appropriate dir given the type chosen.')

(options, args) = parser.parse_args()

#initialize option vars
throttle = options.throttle

#make sure we have a valid type
type = options.type
valid_types = ['httpd', 'iptables', 'kdd']
if type not in valid_types:
    type = valid_types[0]

#if logdir option isn't set, then set its default based on value of type
logdir = options.dir if options.dir else '/var/log/httpd/' if type=='httpd' else '/var/log/iptables/' if type=='iptables' else '../data/' if type=='kdd' else '/tmp/cisdata/'

#set file name to write to, based on type
logfile = 'access_log' if type=='httpd' else 'iptables.log' if type=='iptables' else 'kddcup.testdata.unlabeled' if type=='kdd' else 'cis.log'

print 'options =', options
print 'logdir =', logdir
print 'logfile =', logfile
print 'type =', type
print 'throttle =', throttle


################################
#Code to read data templates to test
inputfilepath = logdir + logfile
with open(inputfilepath) as inputfile:
    testdata = inputfile.read().splitlines()
print 'Loaded ', len(testdata), ' lines for testing from ', inputfilepath


################################
#i/o code and functions

#return a line of log file data, with some elements randomized
def getline():
    templatelist = linetemplates[type]
    return choice(templatelist)


################################
#main loop

#make test requests until we are killed
urlprefix = 'http://hadoop0:8091/assign/'
starttime = time.time()
while 1:
    line = choice(testdata)

    req = urllib2.Request(urlprefix + line, None, {})
    res = urllib2.urlopen(req).read()
    totalrequests += 1

    #if res != '0':
    print res + '->' + line

    #just for testing
    #if totalrequests >= 100:
    #    sys.exit()

    if totalrequests % 1000 == 0:
        print 'Rate = ' + str(totalrequests / (time.time() - starttime)) + ' per sec'

    #sleep periodically to throttle; the second clause in the condition is there because even a very short
    #sleep introduces enough overhead to throttle too much; so doing multiple writes before the sleep
    #effectively counteracts that
    if throttle > 0 and totalrequests % writespersleep == 0:
        time.sleep(throttle / 1000)
