#!/usr/bin/env bash

#REM set the class path,
#REM assumes the build was executed with maven copy-dependencies
export BASE_CP=app/target/aisafe.app-1.4.0-SNAPSHOT.jar:app/target/dependency/*;

#REM call the java VM
java -cp $BASE_CP eapli.aisafe.bootstrap.AISafeBootstrapApp -bootstrap:demo
