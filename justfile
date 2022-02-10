#!/usr/bin/env just --justfile

# build the project
build:
  mvn -DskipTests clean package

# install into local maven repository
install:
  rm -rf ~/.m2/repository/org/mvnsearch/jbang-spring-native
  mvn -DskipTests clean package install

# hello test
hello: install
  jbang --verbose demo/src/hello/Hello.java

# hello native test
hello-native: install
  jbang --native --verbose demo/src/hello/Hello.java