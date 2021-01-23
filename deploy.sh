#!/usr/bin/env bash

#echo "Switch to production database settings"
#exit

HOST=root@hydev.org

rm -f ./build/libs/*
gradle bootJar
scp ./build/libs/clock_api.jar $HOST:/app/depl/clock-api
ssh $HOST "systemctl restart clock-api"
