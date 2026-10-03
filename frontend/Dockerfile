FROM node:22-alpine AS build
WORKDIR /workspace
COPY package*.json ./
RUN npm ci
COPY angular.json tsconfig*.json ./
COPY public ./public
COPY src ./src
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27.5-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/frontend/browser/ /usr/share/nginx/html/
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=6 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
