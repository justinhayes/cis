#! /usr/bin/env python

import os
import sys
import time
from optparse import OptionParser
from random import choice
from string import Template

################################
#some global vars

totalbyteswritten = 0
currbyteswritten = 0
fileswritten = 0
totallineswritten = 0
writespersleep = 1000
tmplogdir = '/tmp/'


################################
#parse options

parser = OptionParser()
parser.add_option('-t', '--throttle', dest='throttle', default=100, type='float', help='How much to throttle writing by; interpreted as number of ms to sleep after each write.')
parser.add_option('-y', '--type', dest='type', default='httpd', help='Type of log data to write: "httpd", "iptables", "kdd".')
parser.add_option('-d', '--dir', dest='dir', help='Directory to write the log file data into; defaults to the appropriate dir given the type chosen.')
parser.add_option('-m', '--max', dest='maxfilesize', default=(1024*1024), type='int', help='Max file size of the log files, above which they get rolled.')

(options, args) = parser.parse_args()

#initialize option vars
throttle = options.throttle
maxfilesize = options.maxfilesize

#make sure we have a valid type
type = options.type
valid_types = ['httpd', 'iptables', 'kdd']
if type not in valid_types:
    type = valid_types[0]

#if logdir option isn't set, then set its default based on value of type
logdir = options.dir if options.dir else '/var/log/httpd/' if type=='httpd' else '/var/log/iptables/' if type=='iptables' else '/var/log/kdd/' if type=='kdd' else '/tmp/cisdata/'

#set file name to write to, based on type
logfile = 'access_log' if type=='httpd' else 'iptables.log' if type=='iptables' else 'kdd.log' if type=='kdd' else 'cis.log'

print 'options =', options
print 'logdir =', logdir
print 'logfile =', logfile
print 'type =', type
print 'throttle =', throttle
print 'maxfilesize =', maxfilesize


################################
#functions to make fake values and replace them in the dummy data

#function to generate a fake source IP address
def makesrcip():
    return '192.168.'+`choice(range(0,100))`+'.'+`choice(range(0,100))`

#function to generate a fake destination IP address
def makedstip():
    return '192.168.'+`choice(range(100,200))`+'.'+`choice(range(100,200))`

#function to generate a fake source port
def makesrcport():
    return `choice(range(40000,41000))`

#function to generate a fake destination port
def makedstport():
    return `choice(range(20,100))`

#function to generate a random month: either 'Jul' or 'Aug'
def makemonth():
    return choice(['Jul', 'Aug'])

#function to generate a random day, betweeon 1 and 30, inclusive
def makeday():
    return `choice(range(1,31))`

#function to return a time of the format hh:mm:ss, where 00<=hh<=23, 00<mm<59, and 00<ss<59
def maketime():
    hour = choice(range(24))
    hour = `hour` if hour>10 else '0'+`hour`
    min = choice(range(60))
    min = `min` if min>10 else '0'+`min`
    sec = choice(range(60))
    sec = `sec` if sec>10 else '0'+`sec`
    return hour+':'+min+':'+sec

#function to return a protocol
def makeproto():
    return choice(('tcp','udp','icmp','snmp','rdp','ip','ipv6'))

#function to generate a random length, between 0 and 10000, inclusive
def makelen():
    return `choice(range(0,10000))`

#function to replace host/ip (everything up to the first ' -') in an httpd access_log line with a fake version
def replacesrcinhttpd(line):
    idx = line.find(' -')
    return makesrcip()+line[idx:]

#predicate that returns true if this is a valid httpd access_log line
def isvalidhttpd(line):
   return len(line.split(' ')) == 10


################################
#Code to prepare templates for dummy data

#httpd: load access log lines from a sample file
httpdtemplates = []
if type=='httpd':
    #nasa_access_log for non-normalized version if you are going to normalize it now
    httpdsamplefile = '../data/nasa_access_log_norm'
    #httpdsamplefile = '../data/nasa_access_log'

    with open(httpdsamplefile) as httpdfile:
        httpdtemplates = httpdfile.read().splitlines()

    #normalize the source host/ip to the common range of fake ip addresses; just do once and write
    #httpdtemplates = filter(isvalidhttpd, httpdtemplates)
    #httpdtemplates = map(replacesrcinhttpd, httpdtemplates)
    #outfile = open('/tmp/nasa_access_log_norm2', 'w')
    #print 'writing updated data file'
    #for item in httpdtemplates:
    #    print>>outfile, item
    #outfile.close()

    print 'Loaded', len(httpdtemplates), 'httpd lines from', httpdsamplefile


#iptables: use a pattern that will be repeated many times, with tokens replaced with fake values
iptablestemplates = []
if type=='iptables':
    for i in range(0,100000):
        linetemplate = Template('$month  $day $time debian kernel: IN=ra0 OUT= MAC=00:17:9a:0a:f6:44:00:08:5c:00:00:01:08:00 SRC=$srcip DST=$dstip LEN=$len TOS=0x00 PREC=0x00 TTL=51 ID=18374 DF PROTO=$proto SPT=$srcport DPT=$dstport WINDOW=5840 RES=0x00 SYN URGP=0')
        iptablestemplates.append(linetemplate.substitute(month=makemonth(), day=makeday(), time=maketime(), srcip=makesrcip(), dstip=makedstip(), len=makelen(), proto=makeproto(), srcport=makesrcport(), dstport=makedstport()))
    print 'Created ', len(iptablestemplates), 'sample iptables lines'


#kdd: load lines from a sample file (if we are doing a kdd run)
kddtemplates = []
if type=='kdd':
    #can be kddcup.testdata.unlabeled or kddcup.testdata.unlabeled_10_percent
    kddsamplefile = '../data/kddcup.testdata.unlabeled'
    with open(kddsamplefile) as kddfile:
        kddtemplates = kddfile.read().splitlines()
    print 'Loaded', len(kddtemplates), 'kdd lines from', kddsamplefile

linetemplates = {
    'httpd': httpdtemplates,
    'iptables': iptablestemplates,
    'kdd': kddtemplates
}


################################
#i/o code and functions

#create logdir if necessary
if not os.path.exists(logdir):
    print 'Creating', logdir
    os.makedirs(logdir)


#open a file
def openfile(name):
    return open(name, 'w')

    
#close a file
def closefile(handle):
    if not handle.closed:
        handle.close()
        print 'Closed', handle.name


#write a line to the log file, rotating if necessary
def writeline(handle, line):
    global totalbyteswritten, currbyteswritten, fileswritten
    #print 'writing', line, 'to', handle.name
    
    #roll the file if necessary
    if currbyteswritten >= maxfilesize:
        #newname = handle.name + '.' + `fileswritten`
        newname = logdir + logfile + '.' + `fileswritten`
        fileswritten += 1
        currbyteswritten = 0
        print 'Renaming', handle.name, 'to', newname
        closefile(handle)
        os.rename(handle.name, newname)
        handle = openfile(handle.name)
    
    handle.write(line + '\n')
    totalbyteswritten += len(line) + 1
    currbyteswritten += len(line) + 1
    
    return handle


#return a line of log file data, with some elements randomized
def getline():
    templatelist = linetemplates[type]
    return choice(templatelist)


################################
#main loop

#open up the initial file
#f = open(logdir+logfile, 'w')
f = open(tmplogdir+logfile, 'w')

#write until we are killed
while 1:
    f = writeline(f, getline())
    totallineswritten += 1
    #sleep periodically to throttle; the second clause in the condition is there because even a very short
    #sleep introduces enough overhead to throttle too much; so doing multiple writes before the sleep
    #effectively counteracts that
    if throttle > 0 and totallineswritten % writespersleep == 0:
        time.sleep(throttle / 1000)


#clean up
closefile(f)

