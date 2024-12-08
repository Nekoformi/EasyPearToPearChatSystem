#!/bin/bash

displayMarginTop=32
displayMarginLeft=0
displayMarginBottom=0
displayMarginRight=0
displayOffsetX=0
displayOffsetY=0
initialPort=20000

clusterNumber="$1"
clusterColumn="$2"
clusterRow="$3"
displayWidth="$4"
displayHeight="$5"
timeout="$6"
beacon="$7"
initialJoinAddressAndPort="$8"

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

if [[ -z "$timeout" ]]; then
    timeout=30000
fi

if [[ -z "$beacon" ]]; then
    beacon=160000
fi

clusterNumberDisplay=`printf "%04d" "$clusterNumber"`
clusterSize=$((clusterColumn*clusterRow))

screenWidth=$((displayWidth-(displayMarginLeft+displayMarginRight)))
screenHeight=$((displayHeight-(displayMarginTop+displayMarginBottom)))

windowWidth=$((screenWidth/clusterColumn))
windowHeight=$((screenHeight/clusterRow))

logTime=$(date +%Y-%m-%d_%H-%M-%S)

for i in `seq 0 $((clusterSize-1))`; do
    posX=$((i%clusterColumn))
    posY=$((i/clusterColumn))

    windowPosX=$((windowWidth*posX+displayMarginLeft+displayOffsetX))
    windowPosY=$((windowHeight*posY+displayMarginTop+displayOffsetY))

    userNumber="$((i+1))"
    userNumberDisplay=`printf "%04d" "$userNumber"`
    userName="C${clusterNumberDisplay}_U${userNumberDisplay}"
    userId=`printf "@%04x%04x000000000000000000000000" "$((clusterNumber-1))" "$i"` # {C:4}{U:4}{0x000000000000000000000000}

    port=$((initialPort+i+1))
    mainPort=$((initialPort+(posY-1)*clusterColumn+1))
    subPort=$((initialPort+posY*clusterColumn+1))

    userFolder=./"Test/${userName}"
    logFile="${userFolder}/${logTime}.log"

    if [ ! -d "$userFolder" ]; then
        mkdir -p "$userFolder"
    fi

    if [ $i -eq 0 ]; then
        if [[ -z "$initialJoinAddressAndPort" ]]; then
            java -jar ./Test/E=CS.jar -n="$userName" -i="$userId" -d="$userFolder" \
            -x="$windowPosX" -y="$windowPosY" -w="$windowWidth" -h="$windowHeight" \
            -t="$timeout" -b="$beacon" \
            -create="$port" -ssl -debug > "$logFile" &
        else
            java -jar ./Test/E=CS.jar -n="$userName" -i="$userId" -d="$userFolder" \
            -x="$windowPosX" -y="$windowPosY" -w="$windowWidth" -h="$windowHeight" \
            -t="$timeout" -b="$beacon" \
            -join="${initialJoinAddressAndPort},${port}" -ssl -debug > "$logFile" &
        fi
    elif [ $posX -eq 0 ]; then
        java -jar ./Test/E=CS.jar -n="$userName" -i="$userId" -d="$userFolder" \
        -x="$windowPosX" -y="$windowPosY" -w="$windowWidth" -h="$windowHeight" \
        -t="$timeout" -b="$beacon" \
        -join="0.0.0.0:${mainPort},${port}" -ssl -debug > "$logFile" &
    else
        java -jar ./Test/E=CS.jar -n="$userName" -i="$userId" -d="$userFolder" \
        -x="$windowPosX" -y="$windowPosY" -w="$windowWidth" -h="$windowHeight" \
        -t="$timeout" -b="$beacon" \
        -join="0.0.0.0:${subPort},${port}" -ssl -debug > "$logFile" &
    fi

    sleep 15
done
