#!/bin/bash

#This installs the Cloudera InfoSec Solution


###Set up the vars
CIS_HOME=/root/cis
SOLR_CONF_HOME=$CIS_HOME/conf/solrconf
ZK_QUORUM=hadoop0:2181


###Initial environment setup
mkdir -p $CIS_HOME/conf
mkdir -p $CIS_HOME/bin
mkdir -p $CIS_HOME/oryx
mkdir -p $CIS_HOME/data
cp -R $CIS_HOME/package/src/main/conf/flume $CIS_HOME/conf/
cp -R $CIS_HOME/package/src/main/conf/impala $CIS_HOME/conf/
cp -R $CIS_HOME/package/cis-0.0.1-SNAPSHOT.jar $CIS_HOME/conf/flume
cp -R $CIS_HOME/package/src/main/scripts/* $CIS_HOME/bin
cp -R $CIS_HOME/package/oryx/* $CIS_HOME/oryx/
cp -R $CIS_HOME/package/data/* $CIS_HOME/data/
gunzip $CIS_HOME/data/kddcup.data_10_percent.csv.gz
gunzip $CIS_HOME/data/kddcup.testdata.unlabeled.gz
gunzip $CIS_HOME/data/nasa_access_log_norm.gz


###Initial search setup
#generate the solr config files
solrctl --zk $ZK_QUORUM/solr instancedir --generate $SOLR_CONF_HOME
#overlay our customizations on top of the sold config files
yes | cp -Rf $CIS_HOME/package/src/main/conf/solrconf/* $SOLR_CONF_HOME


###Initialize the rest of the hadoop environment
chmod +x $CIS_HOME/bin/init-hadoop.sh
/bin/sh $CIS_HOME/bin/init-hadoop.sh


###Initialize things for Oryx
#Create the Oryx system user (if necessary) - TBD
#Create required HDFS directory
hadoop fs -rm -r /user/oryx/ciskmeans/*
hadoop fs -mkdir -p /user/oryx/ciskmeans/00000/inbound
#Copy the KDD cup training data to HDFS
hadoop fs -copyFromLocal $CIS_HOME/data/kddcup.data_10_percent.csv /user/oryx/ciskmeans/00000/inbound
