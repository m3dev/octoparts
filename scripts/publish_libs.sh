#!/bin/bash

sbt \
++2.10.4 \
clean \
auth-plugin-api/publishSigned \
java-client/publishSigned \
models/publishSigned \
play-json-formats/publishSigned \
scala-ws-client/publishSigned \
++2.11.5 \
clean \
auth-plugin-api/publishSigned \
java-client/publishSigned \
models/publishSigned \
play-json-formats/publishSigned \
scala-ws-client/publishSigned

