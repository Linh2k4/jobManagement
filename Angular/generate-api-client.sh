#!/bin/bash

# Script to generate Angular HTTP client from Swagger/OpenAPI spec
# Usage: ./generate-api-client.sh (before running, make sure backend is running on http://localhost:8080)

SWAGGER_URL="http://localhost:8080/v3/api-docs"
OUTPUT_DIR="src/app/core/generated-api"

echo "🔄 Generating API client from Swagger spec..."
echo "📍 URL: $SWAGGER_URL"
echo ""

# Check if backend is accessible
if ! curl -s "$SWAGGER_URL" > /dev/null 2>&1; then
    echo "❌ ERROR: Cannot reach backend at $SWAGGER_URL"
    echo "Make sure backend is running:"
    echo "  cd ../../Java/JobManagement"
    echo "  mvn -pl api spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=dev'"
    exit 1
fi

echo "✅ Backend is accessible"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Generate API client
npx @openapitools/openapi-generator-cli generate \
  -i "$SWAGGER_URL" \
  -g typescript-angular \
  -o "$OUTPUT_DIR" \
  --additional-properties="ngVersion=18.0.0" \
  --additional-properties="apiModulePrefix=Api" \
  --additional-properties="withInterfaces=true"

echo ""
echo "✅ API client generated in $OUTPUT_DIR"
echo ""
echo "Generated files:"
ls -la "$OUTPUT_DIR" | grep -E "\.ts$|\.js$" | head -10
echo ""
echo "📚 Generated structure:"
echo "  src/app/core/generated-api/"
echo "  ├── models/          (Data types: Task, User, Evaluation, etc.)"
echo "  ├── services/        (HTTP services: TaskService, UserService, etc.)"
echo "  └── api.ts          (API client configuration)"
echo ""
echo "✨ Next steps:"
echo "1. Import services in your components:"
echo "   import { TaskService } from '@app/core/generated-api/services';"
echo ""
echo "2. Inject in constructor:"
echo "   constructor(private taskService: TaskService) {}"
echo ""
echo "3. Use API methods:"
echo "   this.taskService.getAll().subscribe(data => { ... });"
