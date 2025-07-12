#!/bin/sh
export GRADLE_HOME=`dirname $0`/gradle
$GRADLE_HOME/bin/gradle "$@"
