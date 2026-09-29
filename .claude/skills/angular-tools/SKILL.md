---
name: angular-tools
description: Angular CLI command reference for JobManagement FE (build, test, lint, API client generation). Trigger on build, test, or generated-api tasks in the Angular project.
---

# Angular Tools & Commands

## Dev server

```bash
npm start          # ng serve, http://localhost:4200
ng serve --port 4300
```

## Build

```bash
npm run build       # production build -> dist/job-management
ng build --configuration development
npm run watch        # incremental build, watch mode
```

## Testing

```bash
npm test             # ng test (Karma + Jasmine)
ng test --code-coverage
```

## API client (OpenAPI codegen from backend Swagger)

Backend must be running first (`Java/JobManagement`, module `api`, port 8080).

```bash
./generate-api-client.sh
# or from workspace root:
../../.claude/scripts/sync-fe-be.sh
```

See `API_GENERATION.md` in this project and `docs/ARCHITECTURE.md` (root) — section "FE-BE contract" — for the full flow.

## Common issues

| Issue | Solution |
|-------|----------|
| `generate-api-client.sh` fails to reach backend | Confirm `curl -s http://localhost:8080/v3/api-docs` returns JSON |
| Generated client stale after backend DTO change | Re-run `generate-api-client.sh`, generated code is gitignored |
| Port 4200 already in use | `ng serve --port 4300` |
