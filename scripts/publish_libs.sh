#!/bin/bash

sbt \
++2.10.6 \
clean \
auth-handler-api/publishSigned \
java-client/publishSigned \
models/publishSigned \
play-json-formats/publishSigned \
scala-ws-client/publishSigned \
++2.11.8 \
clean \
auth-handler-api/publishSigned \
java-client/publishSigned \
models/publishSigned \
play-json-formats/publishSigned \
scala-ws-client/publishSigned
