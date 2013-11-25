Introduction
===========
The Cloudera InfoSec Solution (CIS) is a reference architecture for integrating several Hadoop ecosystem components
together into a system that can be used for InfoSec use cases. It provides the following high level features:
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
- ZoomData
- Apache Hue

---

At a high level, the flow of data through the system is as follows:

1. Data arrives as log files in a log directory (the datadriver.py script populates these log files with synthetic data)
2. A Flume agent reads the log file data, parses it, normalizes it, and 
    - writes it to HDFS, and 
    - indexes it in Cloudera Search, and
    - sends it to the machine learning component [TBD]
3. A partitioned Impala table is defined over the HDFS data, enabling SQL querying
4. Cloudera Search provides search on the indexed event data
5. The machine learning component uses the event data to update its model(s) determine if it is anomalous [TBD]
6. ZoomData provides historical and streaming visualization of the data via Cloudera Impala and Search [TBD]
7. ZoomData provides near real time alerting of events flagged as anomalous [TBD]


Getting Started
---------------
To install the CIS, you must have a CDH cluster, with the above components (and their dependencies) all running
and fully functional. Then, follow these steps:  

1. Create a directory to install CIS to; this directory is referred to as $CIS_HOME (no need to create the env var though)
    - Ensure that the paths in src/main/config/flumeconf/flume.conf and the scripts in src/main/scripts match the directory you just created
    - Ensure that the ZK_QUORUM property in src/main/scripts/install.sh and init-hadoop.sh is correct for your cluster
2. Build the CIS jar file via: mvn clean install
3. Copy the src directory and target/cis-0.0.1-SNAPSHOT.jar into a local directory called 'package', tar it, and copy it to the $CIS_HOME on the server
4. Untar the package tar file in the $CIS_HOME directory 
5. Run the $CIS_HOME/package/src/main/scripts/install.sh script, which will:
    a. Create required directories, all underneath $CIS_HOME
    b. Copy the config files, scripts, and other files into the appropriate directories
    c. Initialize Cloudera Search and copy the Solr config files into the appropriate place under $CIS_HOME/solrconf
    d. Create the Impala table and pre-create the required partitions


Running the Solution
--------------------
To run the application, ensure that all CDH services are operating normally and then enter the following commands, each in its own shell:
```
    $> flume-ng agent -c $CIS_HOME/config/flumeconf -f $CIS_HOME/config/flumeconf/flume.conf -n cis
    $> python $CIS_HOME/scripts/datadriver.py -yhttpd -t100
```

These processes can be run in the background as well. Additionally, multiple instances of the datadriver.py script can be run (one per type)
to generate each type of event data. Run 'python $CIS_HOME/scripts/datadriver.py -h' for script options.

Once these processes are running, you can run Impala queries and Solr searches via Hue, at http://<host>:8888. You can also 
view the ZoomData visualizations at http://<host>:????.

 

