# syntax=docker/dockerfile:1
FROM node:24-alpine AS build
WORKDIR /app
COPY frontend/cinemind-angular/package*.json ./
RUN npm ci
COPY frontend/cinemind-angular/ ./

# The gateway's browser-visible URL is injected at build time (never a secret).
ARG API_BASE_URL=http://localhost:8080
RUN printf "export const environment = { production: true, apiBaseUrl: '%s' };\n" \
      "$API_BASE_URL" > src/environments/environment.production.ts \
 && npm run build

FROM nginx:alpine
COPY frontend/cinemind-angular/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/cinemind-angular/browser /usr/share/nginx/html
EXPOSE 80
