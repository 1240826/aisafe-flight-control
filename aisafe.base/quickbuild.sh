#!/usr/bin/env bash
# Build all modules (skip javadoc, copy dependencies for running)
mvn -B $1 verify dependency:copy-dependencies -D maven.javadoc.skip=true
