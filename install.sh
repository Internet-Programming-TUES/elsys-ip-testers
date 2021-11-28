#!/bin/sh
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file  \
    -Dfile=target/testers-1.0-SNAPSHOT.jar \
    -DpomFile=pom.xml \
    -DlocalRepositoryPath=/Users/ivaylomihaylov/tues/git/elsys-ip-homework-2021/lib