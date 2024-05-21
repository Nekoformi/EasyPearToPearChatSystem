#!/bin/bash

java -jar E=CS.jar -n="Test User A" -x=0 -y=60 -create=20000 -ssl &
java -jar E=CS.jar -n="Test User B" -x=640 -y=60 -ssl &
java -jar E=CS.jar -n="Test User C" -x=1280 -y=60 -ssl &
java -jar E=CS.jar -n="Test User D" -x=0 -y=540 -ssl &
java -jar E=CS.jar -n="Test User E" -x=640 -y=540 -ssl &
java -jar E=CS.jar -n="Test User F" -x=1280 -y=540 -ssl &
