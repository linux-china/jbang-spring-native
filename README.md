JBang build integration with Spring Native
==========================================

![JBang Spring Native](./jbang-spring-native.png)

# How to use?

* Install GraalVM with native-image and set up `GRAALVM_HOME` env variable
* Create JBang script with `org.mvnsearch:jbang-spring-native:2.7.0` DEPS

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//DEPS org.mvnsearch:jbang-spring-native:2.7.0
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
        System.out.println("Hello Spring Native!");
    }
}
```

* Execute `jbang --native Hello.java`

# AOT configuration

```
//AOT:CONFIG removeXmlSupport=true removeSpelSupport=true
//AOT:CONFIG removeYamlSupport=false
```

# Docker image build with jbang-spring-native

Create `Dockerfile` with following code:

```dockerfile
FROM linuxchina/jbang-action:0.94.0-graal-java17-22.1.0 as builder

ARG mainClass="SpringBootApp.java"

RUN mkdir -p /opt/app/out
WORKDIR /opt/app

COPY $mainClass /opt/app

RUN jbang export portable --native -O out/main $mainClass
#RUN upx -7 out/main

FROM paketobuildpacks/run:tiny-cnb

COPY --from=builder /opt/app/out /opt/app/out

ENTRYPOINT ["/opt/app/out/main"]
```

Then execute `docker build -t your_name/spring-boot-native . ` to create Docker image.

# References

* JBang Build Integration: https://www.jbang.dev/documentation/guide/latest/integration.html
* Spring Native: https://docs.spring.io/spring-native/docs/0.12.0/reference/htmlsingle/
* Native Image Build Configuration: https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/