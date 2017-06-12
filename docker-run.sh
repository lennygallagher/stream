#!/usr/bin/env bash

mvn clean install
docker build -t tom/kafka-streams:1.0.0 .
docker stop kafka-streams && docker rm kafka-streams
docker run --name kafka-streams --net=hackathon -p 8082:8080 tom/kafka-streams:1.0.0
docker logs kafka-streams -f