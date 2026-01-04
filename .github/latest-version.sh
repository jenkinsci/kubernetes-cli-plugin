#!/bin/bash

major=1
for minor in `seq 32 50`; do
    for patch in `seq 0 50`; do
        code=`curl -s -I https://dl.k8s.io/release/v$major.$minor.$patch/bin/linux/amd64/kubectl -o /dev/null -L -w "%{http_code}"`
        if [ "$code" != "200" ]; then
            if [ "$patch" = "0" ]; then
                echo "No more versions available for $major.$minor onwards"
                exit 0
            fi
            echo "Latest version for $major.$minor is: v$major.$minor.$(($patch-1))"
            break
        fi
    done
done
