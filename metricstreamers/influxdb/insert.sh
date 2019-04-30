#!/bin/bash

#
# Script for populating an InfluxDB database with some sample data.
# 

set -e

dbhost=localhost
dbport=8086
database=mydb
measurement=cpu

curl -i -XPOST http://${dbhost}:${dbport}/query --data-urlencode "q=CREATE DATABASE mydb"

#
# Fill an InfluxDB database with data points
#

now_nanos=$(( $(date +%s) * 1000 * 1000 * 1000 ))
# one hour
start_nanos=$(( ${now_nanos} - 1 * 3600 * 1000 * 1000 * 1000 ))
# 20 seconds between measurements
step_nanos=$(( 20 * 1000 * 1000 * 1000 ))
for region in useast eucentral euwest; do
    for host in host1 host2 host3; do
	for t in $(seq ${start_nanos} ${step_nanos} ${now_nanos}); do
	    value=$RANDOM
	    curl -XPOST http://${dbhost}:${dbport}/write?db=${database} --data-binary "${measurement},host=${region}.${host},region=${region} value=${value} ${t}"
	done
    done
done
