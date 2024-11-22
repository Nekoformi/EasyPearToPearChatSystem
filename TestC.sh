#!/bin/bash

displayMarginTop=32
displayMarginLeft=0
displayMarginBottom=0
displayMarginRight=0
initialPort=20000

clusterNumber="$1"
clusterColumn="$2"
clusterRow="$3"
displayWidth="$4"
displayHeight="$5"

if [[ -z "$clusterNumber" ]]; then
    clusterNumber=0
fi

if [[ -z "$clusterColumn" ]]; then
    clusterColumn=4
fi

if [[ -z "$clusterRow" ]]; then
    clusterRow=4
fi

if [[ -z "$displayWidth" ]]; then
    displayWidth=1920
fi

if [[ -z "$displayHeight" ]]; then
    displayHeight=1080
fi

clusterNumber=`printf "%04d" "$clusterNumber"`
clusterSize=$((clusterColumn*clusterRow))

screenWidth=$((displayWidth-(displayMarginLeft+displayMarginRight)))
screenHeight=$((displayHeight-(displayMarginTop+displayMarginBottom)))

windowWidth=$((screenWidth/clusterColumn))
windowHeight=$((screenHeight/clusterRow))

logTime=$(date +%Y-%m-%d_%H-%M-%S)

for i in `seq 0 $((clusterSize-1))`; do
    posX=$((i%clusterColumn))
    posY=$((i/clusterColumn))

    windowPosX=$((windowWidth*posX+displayMarginLeft))
    windowPosY=$((windowHeight*posY+displayMarginTop))

    userNumber=`printf "%04d" "$((i+1))"`
    userName="C${clusterNumber}_U${userNumber}"

    port=$((initialPort+i+1))
    mainPort=$((initialPort+(posY-1)*clusterColumn+1))
    subPort=$((initialPort+posY*clusterColumn+1))

    userFolder=./"Test/${userName}"
    logFile="${userFolder}/${logTime}.log"

    if [ ! -d "$userFolder" ]; then
        mkdir -p "$userFolder"
    fi

    if [ $i -eq 0 ]; then
        java -jar ./Test/E=CS.jar -n="$userName" -d="$userFolder" \
        -x="$windowPosX" -y="$windowPosY" -w="$windowWidth" -h="$windowHeight" \
        -create="$port" -ssl -debug > "$logFile" &
        sleep 1
    elif [ $posX -eq 0 ]; then
        java -jar ./Test/E=CS.jar -n="$userName" -d="$userFolder" \
        -x="$windowPosX" -y="$windowPosY" -w="$windowWidth" -h="$windowHeight" \
        -join="0.0.0.0:${mainPort},${port}" -ssl -debug > "$logFile" &
        sleep 1
    else
        java -jar ./Test/E=CS.jar -n="$userName" -d="$userFolder" \
        -x="$windowPosX" -y="$windowPosY" -w="$windowWidth" -h="$windowHeight" \
        -join="0.0.0.0:${subPort},${port}" -ssl -debug > "$logFile" &
        sleep 0.5
    fi
done
