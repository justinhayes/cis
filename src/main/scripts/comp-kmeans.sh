#!/bin/bash

###Set up the vars
CIS_HOME=/root/cis

java -Dconfig.file=$CIS_HOME/conf/oryx/oryx.conf.kmeans -jar $CIS_HOME/oryx/oryx-computation-0.3.0-SNAPSHOT.jar