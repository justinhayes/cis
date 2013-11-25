#!/bin/bash

#This script deletes and recreates the HDFS directories, Impala tables, and Solr collection.

#NOTE: You must first unload the Solr collection via the solr admin ui at http://hadoop2.cloudera.com:8983/solr/#/~cores/
#NOTE: Make sure that the preppartitions.py script is updated to create just the partitions you want. Creation of each 
#partition takes a while, so it is best to create just the ones you need.


###Set up the vars
CIS_HOME=/root/cis
SOLR_CONF_HOME=$CIS_HOME/conf/solrconf
ZK_QUORUM=hadoop0:2181
SOLR_COLLECTION=ciscollection

###HDFS
#It's ok if this fails, which it will the first time this script is run
sudo -u hdfs hadoop fs -rm -R /user/hive/warehouse/cis.db/records/*


###Data dirs
#Prepare the data source dirs
#Note that this will clobber the existing dirs, so first backup any files in those dirs if necessary
for DIR in /var/log/httpd /var/log/iptables /var/log/kdd
do
  if [ -d "$DIR" ]; then
    rm -rf $DIR/*
  else
    mkdir $DIR
  fi
done


###Impala
#Create the database and table
impala-shell -f $CIS_HOME/conf/impala/create-table.imp
#Pre-create the partitions
python $CIS_HOME/bin/preppartitions.py > $CIS_HOME/conf/impala/create-partitions.imp
impala-shell -f $CIS_HOME/conf/impala/create-partitions.imp


###Search
#Recreate the collection
solrctl --zk $ZK_QUORUM/solr instancedir --delete $SOLR_COLLECTION
solrctl --zk $ZK_QUORUM/solr instancedir --create $SOLR_COLLECTION $SOLR_CONF_HOME
solrctl --zk $ZK_QUORUM/solr collection --create $SOLR_COLLECTION
#Now delete any docs already in the index
solrctl --zk $ZK_QUORUM/solr collection --deletedocs $SOLR_COLLECTION
