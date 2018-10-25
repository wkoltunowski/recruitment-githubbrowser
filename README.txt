# README #

This README describes how to get application running.

### What is this repository for? ###

* Githubrepository is a microservice standalone application which allows viewing details of any github repository. It provides simple cacheing and circuit breaker protection.
* Version 1.0

### How do I get set up? ###

* To get application running under IntelliJ IDEA open project as maven project 
* It is required to install lombok plugin and enable annotation processing (follow https://projectlombok.org/setup/intellij) 
* Tests are divided into 3 packages : integration, performance and units that can be executed independently

* Running application from command line: 
 - Go to githubbrowser project folder
 - Build the application using maven ( `mvn clean install` )
 - Run jar using `java -jar githubbrowser-1.0-SNAPSHOT.jar`
 - Type http://localhost:8080/repositories/${owner}/${repository} in browser (eg. http://localhost:8080/repositories/wokol/spring-boot )
 
 
### Left TODOs ###
* Extract integration and performance tests into separate maven projects that can be built independently
* Extend performance tests with specialized performance testing library (eg. https://gatling.io )

