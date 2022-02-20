#!/bin/bash

major=1
for minor in `seq 18 23`; do
    for patch in `seq 1 50`; do
        code=`curl -s -I https://storage.googleapis.com/kubernetes-release/release/v$major.$minor.$patch/bin/linux/amd64/kubectl -o /dev/null -w "%{http_code}"`
        if [ "$code" != "200" ]; then
            echo "Latest version for $major.$minor is: v$major.$minor.$(($patch-1))"
            break
        fi
    done
done
