#!/bin/bash

java -jar ./Test/E=CS.jar -n="Test User A" -x=0 -y=60 -t=5000 -create=20000 -ssl &
sleep 1
java -jar ./Test/E=CS.jar -n="Test User B" -x=640 -y=60 -t=5000 -join=0.0.0.0:20000,20001 -ssl &
sleep 0.1
java -jar ./Test/E=CS.jar -n="Test User C" -x=1280 -y=60 -t=5000 -join=0.0.0.0:20000,20002 -ssl &
sleep 0.1
java -jar ./Test/E=CS.jar -n="Test User D" -x=0 -y=540 -t=5000 -join=0.0.0.0:20000,20003 -ssl &
sleep 0.1
java -jar ./Test/E=CS.jar -n="Test User E" -x=640 -y=540 -t=5000 -join=0.0.0.0:20000,20004 -ssl &
sleep 0.1
java -jar ./Test/E=CS.jar -n="Test User F" -x=1280 -y=540 -t=5000 -join=0.0.0.0:20000,20005 -ssl &
sleep 0.1
