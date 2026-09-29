# Quick Start - Job Management System

## Prerequisites
- Node.js 18+ & npm
- Java 21+ & Maven
- Docker

## 1️⃣ Start Backend

```bash
cd ../Java/JobManagement

# Start database
docker compose up -d

# Run Spring Boot (port 8080)
cd Backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

✅ Backend ready at: `http://localhost:8080`
📚 Swagger UI: `http://localhost:8080/swagger-ui.html`

## 2️⃣ Generate API Client

```bash
cd Angular/JobManagement

# Make script executable
chmod +x generate-api-client.sh

# Generate TypeScript models & services from Swagger
./generate-api-client.sh
```

Generated files in `src/app/core/generated-api/`:
- `models/` → Data types (Task, User, Evaluation, etc.)
- `services/` → HTTP API calls (TaskService, UserService, etc.)
- `api.ts` → Main API export

## 3️⃣ Start Frontend Dev Server

```bash
# Install dependencies (first time only)
npm install

# Start dev server (port 4200)
npm start
```

Open: `http://localhost:4200`

## 4️⃣ Use Generated API Services

Example in component:

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
  loading = true;
  error: string | null = null;

  constructor(private taskService: TaskService) {}

  ngOnInit() {
    this.taskService.getAllTasks().subscribe({
      next: (response) => {
        this.tasks = response.data; // API wraps in ApiResponse<T>
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }
}
```

## 🔄 Workflow for API Changes

When backend API changes (new endpoint, new DTO, etc.):

1. Update backend code & run backend
2. Regenerate API client:
   ```bash
   ./generate-api-client.sh
   ```
3. Update components to use new models/services
4. Frontend automatically gets type-safe API calls

## 📁 Project Structure

```
Project/
├── Java/Job Management/
│   ├── Backend/          (Spring Boot, 5 layers)
│   └── docker-compose.yml
│
└── Angular/Job Management/
    ├── src/app/
    │   ├── core/         (models, services, guards, interceptors)
    │   ├── shared/       (header, sidebar)
    │   ├── features/     (auth, tasks, evaluations, dashboard, notifications)
    │   ├── app.routes.ts (lazy-loaded feature routes)
    │   └── app.config.ts (providers + HttpClient)
    ├── generate-api-client.sh
    └── API_GENERATION.md
```

## 🐛 Troubleshooting

**Backend not starting?**
```bash
# Check if port 8080 is free
lsof -i :8080
# Kill if needed
kill -9 <PID>
```

**Swagger not accessible?**
```bash
# Verify backend is running
curl http://localhost:8080/swagger-ui.html

# Check backend logs for errors
# Should see "Started JobManagementApplication"
```

**Generation script fails?**
```bash
# Make sure backend is accessible
curl http://localhost:8080/v3/api-docs | head -20

# Re-run with verbose output
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-angular \
  -o src/app/core/generated-api
```

**Frontend build errors?**
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
npm start
```

## 📞 Test Accounts

From Backend seed data (password: `admin123`):

| Email | Role | Description |
|-------|------|-------------|
| admin@company.com | MANAGER | System admin |
| lead1@company.com | LEAD | Team lead 1 |
| lead2@company.com | LEAD | Team lead 2 |
| member1@company.com | MEMBER | Team member |
| member2@company.com | MEMBER | Team member |

## ✅ Verify Setup

- [ ] Backend running on port 8080
- [ ] Swagger UI accessible at http://localhost:8080/swagger-ui.html
- [ ] `src/app/core/generated-api/` folder exists with models & services
- [ ] Frontend dev server running on port 4200
- [ ] Navigation works between routes (tasks, evaluations, dashboard, notifications)
- [ ] No TypeScript errors in console

🎉 **Setup complete!** You can now:
- ✅ Call backend APIs with type-safe services
- ✅ Use generated models for form validation
- ✅ Lazy-load feature routes
- ✅ Regenerate API client when backend changes
