#!/bin/bash

BACKBONEDIR=$(cd `dirname $0` && pwd)
cd $BACKBONEDIR

java -jar bin/Configurator.jar
