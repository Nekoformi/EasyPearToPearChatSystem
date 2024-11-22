#!/bin/bash

java -jar ./Test/E=CS.jar -n="User A" -x=0 -y=60 -t=5000 -b=0 -d="./Test/User A" -create=20000 -ssl &
sleep 1
java -jar ./Test/E=CS.jar -n="User B" -x=640 -y=60 -t=5000 -b=0 -d="./Test/User B" -join=0.0.0.0:20000,20001 -ssl &
sleep 0.1
java -jar ./Test/E=CS.jar -n="User C" -x=1280 -y=60 -t=5000 -b=0 -d="./Test/User C" -join=0.0.0.0:20000,20002 -ssl &
sleep 0.1
java -jar ./Test/E=CS.jar -n="User D" -x=0 -y=540 -t=5000 -b=0 -d="./Test/User D" -join=0.0.0.0:20000,20003 -ssl &
sleep 0.1
java -jar ./Test/E=CS.jar -n="User E" -x=640 -y=540 -t=5000 -b=0 -d="./Test/User E" -join=0.0.0.0:20000,20004 -ssl &
sleep 0.1
java -jar ./Test/E=CS.jar -n="User F" -x=1280 -y=540 -t=5000 -b=0 -d="./Test/User F" -join=0.0.0.0:20000,20005 -ssl &
sleep 0.1
