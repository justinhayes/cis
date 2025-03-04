Introduction
===========
The Cloudera InfoSec Solution (CIS) is a reference architecture for integrating several Hadoop ecosystem components
together into a system that can be used for anomaly detection use cases. It provides the following high level features:
- Low latency SQL
- Search
- Streaming and Historical Visualizations
- Machine Learning for Anomaly Detection
- Near Real Time Flagging of Anomalies

---

The core components of the solution are as follows:
- Apache HDFS
- Apache YARN
- Apache Flume
- Cloudera Impala
- Cloudera Search
- Cloudera Oryx
- Apache Hue
- ZoomData

---

At a high level, the flow of data through the system is as follows:

1. Data arrives as log files in a log directory (the datadriver.py script populates these log files with synthetic data)
2. A Flume agent reads the log file data, parses it, normalizes it, and 
    - queries the Oryx serving layer to determine if this event represents an anomaly
    - indexes it in Cloudera Search, and
    - writes it to HDFS, and 
3. A partitioned Impala table is defined over the HDFS data, enabling SQL querying
4. Cloudera Search provides search on the indexed event data
5. ZoomData provides historical and streaming visualization of the data, including flagged anomalies, via Cloudera Impala and Search
6. Oryx uses the event data to periodically update its model [TODO]


Getting Started
---------------
To install the CIS, you must have a CDH cluster, with the above components (and their dependencies) all running
and fully functional. Then, follow these steps to install CIS onto one of the servers in the cluster. This server
must have the Hadoop client configurations deployed to it so it can use HDFS, run the impala shell, run a flume agent, and run Oryx:  

NOTE: if running in the quickstart VM or any server that doesn't use the Oracle JDK by default, make sure that the JVM variable in 
*/usr/lib/solr/bin/zkcli.sh* resolves to the Oracle JDK instead of the GNU jvm. 
Do this by commenting out the existing **JVM="java"** line and adding **JVM=/usr/java/jdk1.6.0_32/bin/java** below it.

1. Create a directory on the target server to install CIS to; this directory is referred to as $CIS_HOME (no need to create the env var though)
    - Ensure that the scripts in src/main/scripts match the directory you just created
    - Ensure that the ZK_QUORUM property in src/main/scripts/install.sh and init-hadoop.sh is correct for your cluster
2. Build the CIS jar file via: mvn clean install
3. Copy the src, oryx, and data directories, as well as target/cis-0.0.1-SNAPSHOT.jar into a local directory called 'package', tar it, and copy it to the $CIS_HOME on the server
4. Untar the package tar file in the $CIS_HOME directory 
5. Run the $CIS_HOME/package/src/main/scripts/install.sh script, which will:
    - Create required directories underneath $CIS_HOME and /var/lib/flume-ng
    - Copy the config files, scripts, and other files into the appropriate directories
    - Initialize Cloudera Search and copy the Solr config files into the appropriate place under $CIS_HOME/conf/solrconf
    - Create the Impala table and pre-create the required partitions
    - Initializes HDFS for use by Oryx and copies the sample clustering training data to the correct location


Flume Configuration
-------------------
Make sure that the flume service is installed via CM and that the flume agent role is running only on the node in which you are installing everything.
If you want the flume agent to run on all nodes, do the "Initial flume setup" steps from install.sh on each node. To configure and start Flume, do the following:

1. In CM, go to Services->flume1; then Configuration->View and Edit
2. Click on "Agent (Default)" on the left
3. Copy the contents of flume.conf into the Configuration File field
4. Enter "cis" in the Agent Name field
5. Ensure that /var/lib/flume-ng/plugins.d is one of the items in the Plugin directories value
6. Click "Save Changes"
7. Click Actions->Start (or Restart if already running) in the top right button to start the service

You can monitor the flume agent log files via CM or via /var/log/flume-ng/flume-cmf-flume1-AGENT-hadoop0.cloudera.com.log.


Oryx Configuration
--------------------
This solution uses Oryx to build a clustering model that is used to identify outliers, or anomalies, in an incoming data stream.
The training data used in the out-of-the-box solution is comprised of network data provided by the KDD-99 conference. Note that this is only used to demonstrate
the usage of the solution, and not to provide a real anomaly detection model. The synthetic data driver included in the solution also
uses a munged version of this KDD Cup data set. The data set can be found at http://kdd.ics.uci.edu/databases/kddcup99/kddcup99.html.
In a real deployment, the training data and then real-time streaming event data (ie from firewall or web server logs) would be specific to a given organization.

In Oryx, there is also the option to automatically rebuild the model as new data comes into the system. The goal is to have the model evolve
as the amount of data it is exposed to grows. This is accomplished via a scheduled job that copies live data into the Oryx training data directory, initiating
a model rebuilding process. Once the model is rebuilt it automatically replaces the prior model in serving real-time requests.

To get the Oryx binaries, follow the directions at https://github.com/cloudera/oryx/wiki/Building-from-Source. There is a copy of these jars in the oryx directory
for convenienve, but it is better to build your own. The src/main/conf/oryx/ directory contains the oryx config file that is used for the computation and serving layers.
It can be modified as necessary (e.g. to change the model.instance-dir or the k-means++ parameters) for a given deployment. Follow the instructions at 
https://github.com/cloudera/oryx/wiki/Installation for information on Hadoop configuration.

The serving and computation layers are run as separate Java processes, via $CIS_HOME/bin/comp-kmeans.sh and $CIS_HOME/bin/serv-kmeans.sh (see "Running the Solution" below). 
The first time that the comp-keans.sh script is run it will take a while (ie at least 15 minutes) to build the initial clustering model and make it
available for serving requests to the flume ingest pipeline.


Running the Solution
--------------------
To run the application, ensure that all CDH services are operating normally and then enter the following commands, each in its own shell.
If this is the first time that the system has been run, wait until the Oryx computation layer (command 1) has completed building its model 
before turning on the data driver. This can take up to several 10s of minutes or more.
The computation layer will output a "Signaling completion of generation 0" message when it is finished.
```
	# Make sure the flume agents are running in CM
    $> sh $CIS_HOME/bin/comp-kmeans.sh
    $> sh $CIS_HOME/bin/serv-kmeans.sh
    $> python $CIS_HOME/bin/datadriver.py -yhttpd -t1000 -m102400
    # kill the process via ctrl-c after it has created a few files
    $> python $CIS_HOME/bin/datadriver.py -yiptables -t1000 -m102400
    # kill the process via ctrl-c after it has created a few files
    $> python $CIS_HOME/bin/datadriver.py -ykdd -t1000 -m102400
    # kill the process via ctrl-c after it has created a few files
```

These processes would be run in the background for anything other than development environments. 
Additionally, multiple instances of the datadriver.py script can be run at the same time (one per type) to generate each type
of event data. Run 'python $CIS_HOME/bin/datadriver.py -h' for script options.

Once these processes are running, you can run Impala queries and Solr searches via Hue, at http://<host>:8888. You can also 
view the ZoomData visualizations at http://<host>/zoomdata/index.html (once you configure ZoomData to connect to CDH; see below for that).

 
ZoomData Configuration
----------------------
Note: This configuration is incomplete and likely outdated.

If using the ZoomData quickstart VM, you must install NTP to make sure that the clock is synchronized to the CDH cluster and that it uses US/Pacific timezone.
The *zoomdata* account has sudo permissions and its password is *zoomdata*.

```
    $> sudo ln -s /usr/share/zoneinfo/US/Pacific /etc/localtime
    $> sudo yum -y install ntp
    $> sudo chkconfig ntpd on
    $> sudo ntpdate pool.ntp.org
    $> sudo /etc/init.d/ntpd start
```



Securing your Hadoop Cluster (TBD - these are incomplete and probably incorrect, but are kept here for future reference when this part is picked up again)
----------------------------
Configure Hadoop security to secure the CDH cluster.

1. On the server that acts as the Kerberos Key Distribution Center, or KDC (this can be the name node if it's not on a separate server):
    1. yum install krb5-server krb5-libs krb5-auth-dialog
    2. vim /etc/krb5.conf (change EXAMPLE.COM and example.com to your domain, maintaining capitalization; change kerberos.example.com to the FQDN of this server, set ticket_lifetime and renew_lifetime to 365d)
    3. kdb5_util create -s (enter in the password for the KDC master key)
    4. vim /var/kerberos/krb5kdc/kadm5.acl (change EXAMPLE.COM to your domain, maintaining capitalization)
    5. vim /var/kerberos/krb5kdc/kdc.conf (add 'max_renewable_life = 7d' line to the [realms] section; change EXAMPLE.COM and example.com to your domain, maintaining capitalization) ???
    6. service krb5kdc start
    7. service kadmin start
    8. Copy /etc/krb5.conf to all nodes in the cluster, placing it in the /etc directory
    9. Install the unlimited strength JCE policy as per http://www.cloudera.com/content/cloudera-content/cloudera-docs/CM4Ent/latest/Configuring-Hadoop-Security-with-Cloudera-Manager/cmchs_JCE_policy_s4.html
    10. (do after turning on hadoop security via CM) kadmin.local ???
        * modprinc -maxrenewlife 90day krbtgt/YOUR_REALM ???
        * modprinc -maxrenewlife 90day +allow_renewable hue/[fqdn_for_kdc_server] ???
    11. Make nodes a gateway for MapReduce1 so they get the updated /etc/hadoop.conf; then "Deploy Client Configurations" ???
2. On each node in the cluster:
    1. yum install krb5-workstation krb5-libs krb5-auth-dialog
    2. Install the unlimited strength JCE policy as per http://www.cloudera.com/content/cloudera-content/cloudera-docs/CM4Ent/latest/Configuring-Hadoop-Security-with-Cloudera-Manager/cmchs_JCE_policy_s4.html

For more information on installing/configuring Kerberos, see https://access.redhat.com/site/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Managing_Smart_Cards/Configuring_a_Kerberos_5_Server.html



