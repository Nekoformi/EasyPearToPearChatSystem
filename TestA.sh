#!/bin/bash

java -jar ./Test/E=CS.jar -n="Alice" -x=300 -y=300 -t=5000 -d="./Test/Alice" -create=20000 -ssl &
sleep 1
java -jar ./Test/E=CS.jar -n="Bob" -x=940 -y=300 -t=5000 -d="./Test/Bob" -join=0.0.0.0:20000,20001 -ssl &
sleep 0.1
