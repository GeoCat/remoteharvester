# CSW-harvester

## Intro

This service provides functionality to extract records from remote Catalog Service for the Web (CSW) interfaces
providing iso19139 records.
Documents are stored in a PostGreSQL database. The history of previous harvest runs is persisted. You can set up a
process to include records
from federated catalogues, which are linked from records in the harvested catalogue.

Development of this program was sponsored by the European Commission - Joint Research Centre (JRC) within Service
Contract NUMBER – 941143 – IPR – 2021 with subject matter "Facilitating a sustainable evolution and maintenance of
the INSPIRE Geoportal", performed in the period 2021-2023.

Contact:
JRC Unit B.6 Digital Economy,
Via Enrico Fermi 2749, 21027 Ispra, Italy;
JRC-INSPIRE-SUPPORT@ec.europa.eu

## Requirements

- Postgres database
- Apache ActiveMQ

## Configuration

- Provide the following variables with the database connection as environment variables:
    - `HARVESTER_DB_HOST`: Database host name.
    - `HARVESTER_DB_USERNAME`: Database user name.
    - `HARVESTER_DB_PASSWORD`: Database user password.

- Provide the following variable with the ActiveMQ connection as an environment variable:
    - `ACTIVEMQ_URL`: ActiveMQ url.

Additionally, can be provided as start up parameters with the `-D` flag, with the property names in lowercase and
replacing the `_` char with `.`. For example:

```
java -jar csw-harvester-0.0.1-SNAPSHOT.jar -Dactivemq.url=tcp://localhost:61616?jms.prefetchPolicy.queuePrefetch\=1
```

## Methods

### POST /api/startharvest (url, longTermTag, [lookForNestedDiscoveryService])

Triggers a harvest process (returns ProcessID).

```
curl \
  -X POST "http://localhost:9999/api/startHarvest" \  
  -H "Content-Type: application/json" \
  -d '{"url":"https://msdi.data.gov.mt/geonetwork/srv/eng/csw","longTermTag":"MT","lookForNestedDiscoveryService":false}'
```

### GET /api/getstatus/{ProcessID}

Retrieves the status of a running process.

```
curl "http://localhost:9999/api/getstatus/aaa-bbb-ccc"
```

The following statuses are returned by the application:

- `CREATING`: the application is scheduling the job to retrieve the metadata.
- `DETERMINING_WORK`: the application is analysing the harvester configuration.
- `WORK_DETERMINED`: the application has analyse the harvester configuration and it is ready to start retrieving the
  metadata from the server.
- `GETTING_RECORDS`: retrieving the metadata from the server.
- `RECORDS_RECEIVED`: the metadata retrieval has finished.

### GET /api/getlog/{ProcessID}

Retrieves the logs of a process.

```
curl "http://localhost:9999/api/getlog/aaa-bbb-ccc"
```

### GET /api/abortprocess/{ProcessID}

Aborts a running process.

```
curl "http://localhost:9999/api/abortprocess/aaa-bbb-ccc"
```

## Build

```
mvn clean package
```

It is created in the `target` folder a `jar` with the application, for example: `csw-harvester-0.0.1-SNAPSHOT.jar`.

## Installation

Execute the application with the following command:

```
java -jar csw-harvester-0.0.1-SNAPSHOT.jar
```

Software requires ActiveMQ and PostGreSQL.

## Configuration

Configuration goes in the config.properties file, or using environment variables.

## License

This software is made available under GPLv2
