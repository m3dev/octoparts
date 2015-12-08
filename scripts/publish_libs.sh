#!/bin/bash

sbt \
++2.10.5 \
clean \
auth-handler-api/publishSigned \
java-client/publishSigned \
models/publishSigned \
play-json-formats/publishSigned \
scala-ws-client/publishSigned \
++2.11.7 \
clean \
auth-handler-api/publishSigned \
java-client/publishSigned \
models/publishSigned \
play-json-formats/publishSigned \
scala-ws-client/publishSigned

