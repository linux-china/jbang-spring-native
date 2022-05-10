///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//DEPS org.mvnsearch:jbang-spring-native:2.7.0-SNAPSHOT
//JAVA_OPTIONS -agentlib:native-image-agent=config-merge-dir=/tmp/native-image-agent

package hello;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Hello implements CommandLineRunner {

    public static void main(String... args) {
        SpringApplication.run(Hello.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Hello JBang with Spring Native!");
    }
}
