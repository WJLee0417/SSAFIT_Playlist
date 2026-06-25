FROM node:22.12.0-alpine AS build

WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/index.html frontend/vite.config.js ./
COPY frontend/src ./src

ARG VITE_API_BASE_URL=/api
ARG VITE_JSP_BASE_URL=http://127.0.0.1:5173
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_JSP_BASE_URL=${VITE_JSP_BASE_URL}

RUN npm run build

FROM nginx:1.27.4-alpine AS runtime

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
