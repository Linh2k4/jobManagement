# syntax=docker/dockerfile:1.7

# ============================================================
# Stage 1: Build Angular application
# ============================================================
FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./

# Use the lockfile strictly so every image is built from the same dependency
# tree, while retaining npm's download cache across builds.
RUN --mount=type=cache,target=/root/.npm \
    npm ci --legacy-peer-deps

COPY . .

RUN npm run build

# ============================================================
# Stage 2: Serve with Nginx
# ============================================================
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=builder /app/dist/job-management/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
