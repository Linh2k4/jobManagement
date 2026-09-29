# API Client Generation from Swagger

This project uses **OpenAPI Generator** to automatically generate TypeScript Angular HTTP client (models + services) from the backend Swagger/OpenAPI spec.

## Generated Files Structure

When you run the generation, you'll get:

```
src/app/core/generated-api/
├── models/
│   ├── api-models.ts              # All data models (Task, User, Evaluation, etc.)
│   ├── task.model.ts
│   ├── user.model.ts
│   ├── evaluation.model.ts
│   └── notification.model.ts
├── services/
│   ├── task.service.ts            # HTTP API calls for tasks
│   ├── user.service.ts
│   ├── evaluation.service.ts
│   ├── notification.service.ts
│   └── dashboard.service.ts
├── api.ts                         # Main API client config
└── api-doc.js                     # Optional: Documentation export (if enabled)
```

**Key files you'll use:**
- `models/` → Type definitions & interfaces matching backend DTOs
- `services/` → Auto-generated Angular services with HttpClient calls
- `api.ts` → Exported classes & modules

## Setup (One-time)

The generator is already installed via `npm install`. To regenerate when backend API changes:

## Steps to Generate API Client

### 1. Start Backend
```bash
cd ../../Java/JobManagement
mvn -pl api spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Backend Swagger UI will be available at: `http://localhost:8080/swagger-ui.html`

### 2. Generate API Client
```bash
cd Angular/JobManagement
chmod +x generate-api-client.sh
./generate-api-client.sh
```

Or manually:
```bash
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-angular \
  -o src/app/core/generated-api \
  --additional-properties="ngVersion=18.0.0"
```

### 3. Generated Files Structure
```
src/app/core/generated-api/
├── models/           # Data models (Task, User, etc.)
├── services/         # API services (TaskService, UserService, etc.)
└── api/              # API client configuration
```

### 4. Use Generated Services

Example in a component:
```typescript
import { Component, OnInit } from '@angular/core';
import { TaskService } from '@app/core/generated-api/services/task.service';
import { Task } from '@app/core/generated-api/models/task.model';

@Component({
  selector: 'app-task-list',
  templateUrl: './task-list.component.html'
})
export class TaskListComponent implements OnInit {
  tasks: Task[] = [];

  constructor(private taskService: TaskService) {}

  ngOnInit() {
    this.taskService.getAll().subscribe(
      (response: Task[]) => {
        this.tasks = response;
      }
    );
  }
}
```

## Notes

- Regenerate API client whenever backend API changes (new endpoints, new DTOs, etc.)
- The generated code is in `.gitignore` - do NOT commit it
- Keep the script handy for future API updates

## Troubleshooting

**Backend Swagger not accessible?**
- Make sure backend is running on `http://localhost:8080`
- Check backend logs for startup errors
- Visit `http://localhost:8080/swagger-ui.html` to verify Swagger UI

**Generation fails?**
- Verify Java 21+ and Maven installed
- Check backend compilation: `mvn clean compile`
- Try calling Swagger directly: `curl http://localhost:8080/v3/api-docs`
