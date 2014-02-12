#!/bin/bash

#rm -rf /tmp/oryx/examplekmeans/*
#mkdir -p /tmp/oryx/examplekmeans/00000/inbound
#cp kddcup.data_10_percent.csv /tmp/oryx/examplekmeans/00000/inbound

hadoop fs -rm -r /user/oryx/ciskmeans/*
hadoop fs -mkdir -p /user/oryx/ciskmeans/00000/inbound
hadoop fs -copyFromLocal kddcup.data_10_percent.csv /user/oryx/ciskmeans/00000/inbound