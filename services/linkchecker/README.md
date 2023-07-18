# INSPIRE Link Checker

## Requirements

- Postgres database
- Apache ActiveMQ

## Configuration

Update the file `application.properties` configure the following properties:

- Harvester database connection:

```
harvesterdb.datasource.url=jdbc:postgresql://localhost:5432/harvest
harvesterdb.datasource.username=postgres
harvesterdb.datasource.password=mysecretpassword
harvesterdb.datasource.driver-class-name=org.postgresql.Driver
```

- Link checker database connection:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/linkchecker2
spring.datasource.username=postgres
spring.datasource.password=mysecretpassword
spring.datasource.driver-class-name=org.postgresql.Driver
```

- JMS:

```
activemq.url=tcp://localhost:61616?jms.prefetchPolicy.queuePrefetch\=1
```

- Application host / port

```
geocat.jettyHost=0.0.0.0
geocat.jettyPort=8888
```

## Build

```
mvn clean package
```

## Run

```
mvn spring-boot:run
```

## Launch the link checker for a harvester using the harvester name

```
curl -X POST "http://localhost:8888/api/startLinkCheck" -H "Content-Type: application/json"  -d '{"longTermTag":"MT"}'
```
