# GeoNetwork remote harvester services

## Introduction

This project contains the following services to harvest the metadata from CSW servers into Geonetwork opensource:

- **csw-harvester**: service that retrieves geo-metadata documents from servers supporting CSW 2.0.2 specification.

- **linkchecker**: service that processes the links between the geo-metadata and the related services to calculate
  several indicators and determine the visibility and downloadability of the service's metadata datasets.

- **ingester**: service that ingests the results of the csw-harvester and linkchecker services so they are available in
  GeoNetwork opensource (harvester console).

- **full-orchestrator**: service used by GeoNetwork opensource that start a harvesting process and monitor it's
  progress.

Currently, only the CSW harvester is supported

## Requirements

It requires to use GeoNetwork opensource (https://github.com/geonetwork/core-geonetwork), branch `csw-remote-harvester`.

Each service describes the specific requirements required.

