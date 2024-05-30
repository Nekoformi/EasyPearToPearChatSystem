#!/bin/bash

rm -rf ./Test/E=CS.jar

if [ -d "./Build" ]; then
    rm -rf ./Build/*
else
    mkdir -p ./Build
fi

if [ -d "./Cache" ]; then
    rm -rf ./Cache/*
else
    mkdir -p ./Cache
fi

# Get Library

libraryArray=`find ./Library -name '*.jar'`
libraryList=$(printf ",%s" "${libraryArray[@]}")
libraryList="${libraryList:1}"

# Extract Library

cp -RTfp ./Library/ ./Cache/
cd ./Cache
find ./ -name '*.jar' -print0 | xargs -0 -i jar -xf {}
cd ..
rsync -a --include='*/' --exclude='*.jar' ./Cache/ ./Build/
rm -rf ./Cache/*

# Build Source

javac -cp "$libraryList": ./Source/Main.java
mkdir ./Build/Source
rsync -a --include='*/' --exclude='*.java' --exclude='*.xcf' --include='*' ./Source/ ./Build/Source/
find ./Source -name '*.class' | xargs rm
find ./Source -type d -empty -delete

# Package Application

cd ./Build
jar -cfm ../Test/E=CS.jar ../Manifest.mf .
cd ..
