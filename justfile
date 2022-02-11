#!/usr/bin/env just --justfile

# build the project
build:
  mvn -DskipTests clean package

# dependency tree
dependencies:
  mvn dependency:tree -Dscope=compile > dependencies.txt

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

dry-run:
   mvn  -DskipTests -DskipLocalStaging=true -P release clean package

deploy:
   mvn  -DskipTests -P release clean package deploy