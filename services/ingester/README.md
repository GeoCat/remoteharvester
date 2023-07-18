# JRC Ingester

## Configuration

The configuration is applied in the file `src/main/resources/application.properties`. When using the Docker image the
same properties can be set passing them as environment variables to the container. You just need to convert them to
upper-case and replace the dots with underscores. For example, `harvester.jdbc.url` -> `HARVESTER_JDBC_URL`.

The following properties should be configured:

- `geonetwork.baseUrl`: Base url of the GeoNetwork application
- `geonetwork.username`: GeoNetwork username for an administration user.
- `geonetwork.password`: GeoNetwork password for an administration user.
- `harvester.jdbc.url`: JDBC connection string for the harvester database.
- `harvester.jdbc.user`: Database username for the harvester database.
- `harvester.jdbc.pass`: Database user password for the harvester database.
- `ingester.jdbc.url`: JDBC connection string for the ingester database.
- `ingester.jdbc.user`: Database username for the ingester database.
- `ingester.jdbc.pass`: Database user password for the ingester database.
- `metadata.jdbc.url`: JDBC connection string for the GeoNetwork database.
- `metadata.jdbc.user`: Database username for the metadata database.
- `metadata.jdbc.pass`: Database user password for the metadata database.
- `activemq.url`: ActiveMQ url.
- `app.jettyHost`: Jetty host.
- `app.jettyPort`: Jetty port.

Example:

```
geonetwork.baseUrl=http://localhost:9090/geonetwork
geonetwork.username=admin
geonetwork.password=admin

jdbc.driverClassName=org.postgresql.Driver
harvester.jdbc.url=jdbc:postgresql://localhost:5432/inspire_harvest
harvester.jdbc.user=postgres
harvester.jdbc.pass=postgres

ingester.jdbc.url=jdbc:postgresql://localhost:5432/inspire_ingest
ingester.jdbc.user=postgres
ingester.jdbc.pass=postgres

metadata.jdbc.url=jdbc:postgresql://localhost:5432/gn4_jrc
metadata.jdbc.user=postgres
metadata.jdbc.pass=postgres

hibernate.hbm2ddl.auto=none
hibernate.physical_naming_strategy=com.geocat.ingester.config.CustomPhysicalNamingStrategy
hibernate.dialect=org.hibernate.dialect.PostgreSQL10Dialect

activemq.url=tcp://localhost:61616?jms.prefetchPolicy.queuePrefetch=1
app.jettyHost=0.0.0.0
app.jettyPort=9999
```

## Build

The build process creates a fat `jar` file with the application and an embedded Tomcat. To build the application:

```
mvn package
```

## Companion services

The folder `extra` contains a `docker-compose.yml` with the required software for running the JRC Ingester:

* Elasticsearch exposing the port `9200` (http://localhost:9200/).
* Kibana exposing the port `5601` (http://localhost:5601/)
* PostgreSQL 13 listiening at port `5432` of localhost. Credentials are `postgres` / `postgres`. These databases are
  created on start-up.
    * `inspire_harvest`
    * `inspire_digest`
    * `gn4_jrc`
* GeoNetwork listening at port `9090` (http://localhost:9090/geonetwork) configured to connect to database `gn4-jrc` and
  local elasticsearch and kibana.
* ActiveMQ listening at port `61616` of localhost for the JMS server and at port `8161` for the web
  UI (`admin` / `admin`)

Data is persisted between runs for GN, Postgresql, Elasticsearch and Activemq.

To use these services go to the folder `aux` where the `docker-compose.yml` is and run:

```shell
cd extra
docker-compose up -d
```

