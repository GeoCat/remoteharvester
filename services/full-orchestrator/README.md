# JRC Orchestrator

## Dependencies

This component depends on the following services:

* A Postgres database.
* An ActiveMQ iinstance.
* Other JRC Geoportal components:
    * Inspire CSW Harvest API.
    * Inspire Linkchecker API.
    * Inspire CSW Ingester API.

## Configuration

The configuration is applied in the file `src/main/resources/application.properties`. When using the Docker image the
same properties can be set passing them as environment variables to the container. You just need to convert them to
uppler-case and replace the dots. For example, `orchestrator.datasource.url` -> `ORCHESTRATOR_DATASOURCE_URL`.

The following properties can be configured:

- `orchestrator.datasource.url`: JDBC connection string for the orchestrator database. For
  example `jdbc:postgresql://localhost:5432/orchestrator`
- `orchestrator.datasource.username`: Database username for the orchestrator database.
- `orchestrator.datasource.password`: Database user password for the orchestrator database.
- `geocat.jettyHost`: Jetty host. For exmaple `0.0.0.0`
- `geocat.jettyPort`: Jetty port. For example `5555`
- `activemq.url`: ActiveMQ url. For example `tcp://localhost:61616?jms.prefetchPolicy.queuePrefetch\=1`
- `harvester.url`: URL of the CSW Harvester component API. For example: `http://localhost:9999/api`
- `linkchecker.url`: URL of the Linkchecker component API. For example: `http://localhost:8888/api`
- `ingester.url`:  URL of the CSW Ingester component API. For example: `http://localhost:10000/api`

Example:

```properties

orchestrator.datasource.url=jdbc:postgresql://localhost:5432/orchestrator
orchestrator.datasource.username=postgres
orchestrator.datasource.password=postgres
orchestrator.datasource.driver-class-name=org.postgresql.Driver


geocat.jettyHost=0.0.0.0
geocat.jettyPort=5555


activemq.url=tcp://localhost:61616?jms.prefetchPolicy.queuePrefetch\=1

harvester.url=http://localhost:9999/api
linkchecker.url=http://localhost:8888/api
ingester.url=http://localhost:10000/api


camel.springboot.message-history=true
camel.springboot.use-mdc-logging=true
camel.springboot.mdc-logging-keys-pattern=*
camel.springboot.auto-startup=true 
#spring.jpa.hibernate.ddl-auto=update
#spring.jpa.properties.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.hbm2ddl.auto=update

spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL10Dialect

spring.jpa.properties.show-sql=false
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

#spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true

logging.level.geocat=INFO
camel.component.bean.scope=request

spring.main.web-application-type=none

```

## Build

### Java

The build process creates a fat `jar` file with the application and an embedded Tomcat. To build the application:

```
mvn package
```

### Docker

The docker image can be run as usual:

```
docker build -t ghrc.io/geonetwork/full-harvester:latest .
```



