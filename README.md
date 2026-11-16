# LifeLane Mini

> AI-Assisted Emergency Dispatch Dashboard — research prototype

Detailed documentation will be added in Phase 12 (task 17.2).

## Repository layout

```
lifelane-mini/
├── backend/        # Spring Boot 3.x REST API (Java 17 + Spring Data JPA)
├── frontend/       # React SPA (TypeScript + Leaflet + MUI)
├── osrm-data/      # OSRM pre-processed map data (not tracked in git)
├── docs/           # OpenAPI spec and architecture diagrams
└── docker-compose.yml
```

## Quick start

### 1. Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Docker Engine 20.10+ and Docker Compose v2+)
- Pre-processed OSRM data in `./osrm-data/` (specifically `addis-ababa.osrm` and associated graph partitions). Follow instructions in `osrm-data/README.md` or execute `scripts/prep-osrm.ps1`.

### 2. Build and Run

To build and start all four services (`postgres`, `osrm`, `backend`, `frontend`) in detached mode:

```bash
docker compose build
docker compose up -d
```

### 3. Verify / Smoke Test Services

Once the containers are running:
- **PostgreSQL**: Verify container health (`lifelane-postgres` reports `healthy` via `pg_isready -U lifelane`).
- **OSRM routing engine**: Test coordinates endpoint at `http://localhost:5000/route/v1/driving/38.75,9.01;38.76,9.02?overview=false` (should return HTTP 200 with route geometry/duration).
- **Backend API**: Check health or fleet endpoint at `http://localhost:8080/api/ambulances` (should return HTTP 200 with seeded ambulances).
- **Frontend SPA**: Access dashboard at `http://localhost:3000` (returns HTTP 200 with the UI dashboard).

To stop all services:
```bash
docker compose down
```

See `docs/` for full prerequisites, environment variables, and OSRM data-preparation steps.
