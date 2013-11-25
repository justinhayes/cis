#!/bin/bash

#This installs the Cloudera InfoSec Solution


###Set up the vars
CIS_HOME=/root/cis
SOLR_CONF_HOME=$CIS_HOME/conf/solrconf
ZK_QUORUM=hadoop0:2181


###Initial environment setup
mkdir -p $CIS_HOME/conf
mkdir -p $CIS_HOME/bin
cp -R $CIS_HOME/package/src/main/conf/flumeconf $CIS_HOME/conf/
cp -R $CIS_HOME/package/src/main/conf/impala $CIS_HOME/conf/
cp -R $CIS_HOME/package/cis-0.0.1-SNAPSHOT.jar $CIS_HOME/conf/flumeconf
cp -R $CIS_HOME/package/src/main/scripts/* $CIS_HOME/bin


###Initial search setup
#generate the solr config files
solrctl --zk $ZK_QUORUM/solr instancedir --generate $SOLR_CONF_HOME
#overlay our customizations on top of the sold config files
yes | cp -Rf $CIS_HOME/package/src/main/conf/solrconf/* $SOLR_CONF_HOME


###Initialize the rest of the hadoop environment
chmod +x $CIS_HOME/bin/init-hadoop.sh
/bin/sh $CIS_HOME/bin/init-hadoop.sh

