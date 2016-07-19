#!/bin/bash

fw_depends java sbt

sbt assembly

java -jar target/scala-2.11/colossus-example-assembly-0.3.0.jar
