#!/usr/bin/env bash

mvn clean install
mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)
docker build -t luminis/norconex-java .
docker tag luminis/norconex-java:latest 044915237328.dkr.ecr.eu-west-1.amazonaws.com/norconex-java:latest
