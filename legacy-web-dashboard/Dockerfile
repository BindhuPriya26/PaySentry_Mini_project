FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL
ARG VITE_WEBSOCKET_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WEBSOCKET_URL=$VITE_WEBSOCKET_URL
RUN case "$VITE_API_BASE_URL" in https://*) ;; *) echo "VITE_API_BASE_URL must use HTTPS" >&2; exit 1 ;; esac \
    && case "$VITE_WEBSOCKET_URL" in wss://*) ;; *) echo "VITE_WEBSOCKET_URL must use WSS" >&2; exit 1 ;; esac \
    && npm run build

FROM nginx:1.27-alpine
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/web-build /usr/share/nginx/html
EXPOSE 8080
