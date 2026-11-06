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

```bash
docker compose up
```

See `docs/` for full prerequisites, environment variables, and OSRM data-preparation steps.
