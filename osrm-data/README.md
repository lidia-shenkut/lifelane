# OSRM Data Preparation

This directory holds the OpenStreetMap map data and pre-processed OSRM routing files for LifeLane Mini.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- An `.osm.pbf` map extract for your target area (e.g., downloaded from [Geofabrik](https://download.geofabrik.de/))

Place the `.osm.pbf` file in this directory before running the commands below.

## Preparation Steps

OSRM requires three processing stages before the routing engine can serve requests. All three stages use the official `osrm/osrm-backend:v5.27` Docker image and mount this directory into the container.

Run the commands from **this directory** (`osrm-data/`).

### Step 1 — Extract

Parses the OSM data and applies the car routing profile:

```powershell
docker run -t -v "${PWD}:/data" osrm/osrm-backend:v5.27 osrm-extract -p /opt/car.lua /data/<file>.osm.pbf
```

Replace `<file>` with the base name of your `.osm.pbf` file (e.g., `ethiopia-latest`).

### Step 2 — Partition

Partitions the road network graph for Multi-Level Dijkstra (MLD) routing:

```powershell
docker run -t -v "${PWD}:/data" osrm/osrm-backend:v5.27 osrm-partition /data/<file>.osrm
```

### Step 3 — Customize

Applies customized edge weights to the partitioned graph:

```powershell
docker run -t -v "${PWD}:/data" osrm/osrm-backend:v5.27 osrm-customize /data/<file>.osrm
```

After all three steps complete, this directory will contain the `.osrm`, `.osrm.ebg`, `.osrm.partition`, `.osrm.cell_metrics`, and related files that the OSRM service reads at startup.

## Automated Script

For convenience, use the helper script at `scripts/prep-osrm.ps1` to run all three stages in sequence:

```powershell
..\scripts\prep-osrm.ps1 -OsmPbfPath ".\<file>.osm.pbf"
```

The script will stop immediately if any stage fails and report which stage errored.

## Docker Compose Integration

The `docker-compose.yml` at the project root starts an OSRM container that reads the pre-processed files from this directory. The service will fail to start if the files are not present — run the data preparation steps above first.

## Gitignore Note

The root `.gitignore` excludes all `*.osrm*` artefacts from version control. Only this `README.md` is committed. Each developer must run the preparation steps locally or obtain the pre-processed files out-of-band.
