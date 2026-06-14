# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.15/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.15/gradle-plugin/packaging-oci-image.html)
* [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/3.5.15/reference/testing/testcontainers.html#testing.testcontainers)
* [Testcontainers Postgres Module Reference Guide](https://java.testcontainers.org/modules/databases/postgres/)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.15/reference/web/servlet.html)
* [Validation](https://docs.spring.io/spring-boot/3.5.15/reference/io/validation.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.5.15/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Flyway Migration](https://docs.spring.io/spring-boot/3.5.15/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
* [Spring Data Redis (Access+Driver)](https://docs.spring.io/spring-boot/3.5.15/reference/data/nosql.html#data.nosql.redis)
* [Spring Security](https://docs.spring.io/spring-boot/3.5.15/reference/web/spring-security.html)
* [OAuth2 Client](https://docs.spring.io/spring-boot/3.5.15/reference/web/spring-security.html#web.security.oauth2.client)
* [WebSocket](https://docs.spring.io/spring-boot/3.5.15/reference/messaging/websockets.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/3.5.15/reference/actuator/index.html)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/3.5.15/specification/configuration-metadata/annotation-processor.html)
* [Docker Compose Support](https://docs.spring.io/spring-boot/3.5.15/reference/features/dev-services.html#features.dev-services.docker-compose)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.5.15/reference/using/devtools.html)
* [SpringDoc OpenAPI](https://springdoc.org/)
* [Spring Modulith](https://docs.spring.io/spring-modulith/reference/)
* [Java Mail Sender](https://docs.spring.io/spring-boot/3.5.15/reference/io/email.html)
* [Testcontainers](https://java.testcontainers.org/)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Messaging with Redis](https://spring.io/guides/gs/messaging-redis/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Using WebSocket to build an interactive web application](https://spring.io/guides/gs/messaging-stomp-websocket/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [SpringDoc OpenAPI](https://github.com/springdoc/springdoc-openapi-demos/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

### Docker Compose support
This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* postgres: [`postgres:latest`](https://hub.docker.com/_/postgres)
* redis: [`redis:latest`](https://hub.docker.com/_/redis)

Please review the tags of the used images and set them to the same as you're running in production.

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/3.5.15/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`postgres:latest`](https://hub.docker.com/_/postgres)
* [`redis:latest`](https://hub.docker.com/_/redis)

Please review the tags of the used images and set them to the same as you're running in production.

