#!/bin/bash

java -jar ./Test/E=CS.jar -n="Test User A" -x=0 -y=60 -t=5000 -create=20000 -ssl &
java -jar ./Test/E=CS.jar -n="Test User B" -x=640 -y=60 -t=5000 -ssl &
java -jar ./Test/E=CS.jar -n="Test User C" -x=1280 -y=60 -t=5000 -ssl &
java -jar ./Test/E=CS.jar -n="Test User D" -x=0 -y=540 -t=5000 -ssl &
java -jar ./Test/E=CS.jar -n="Test User E" -x=640 -y=540 -t=5000 -ssl &
java -jar ./Test/E=CS.jar -n="Test User F" -x=1280 -y=540 -t=5000 -ssl &
