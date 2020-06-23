#!/bin/bash

source /etc/profile

if test $( pgrep -f big-whale | wc -l ) -eq 0
then
  echo "big-whale is not running"
else
  pgrep -f big-whale | xargs kill -9
  echo "big-whale stopped"
fi

curr=$(date "+%Y%m%d")
echo "big-whale starting..."
nohup java -jar big-whale.jar >> application-$curr.log 2>&1 &
