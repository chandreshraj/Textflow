# Build the UI with the same Node version used by the local project.
FROM docker.io/library/node:24.10.0-bookworm-slim AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/index.html frontend/tsconfig.json frontend/vite.config.ts ./
COPY frontend/src ./src
RUN npm test && npm run build

# Maven packages the UI into the executable Spring Boot JAR.
FROM docker.io/library/eclipse-temurin:25.0.4.1_1-jdk-noble AS backend-build
WORKDIR /workspace/backend
COPY backend/ ./
COPY --from=frontend-build /workspace/frontend/dist /workspace/frontend/dist
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress verify

# Only the Java runtime and application are needed in the final image.
FROM docker.io/library/eclipse-temurin:25.0.4.1_1-jre-noble AS runtime
WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /workspace/backend/web-api/target/web-api-1.0.0.jar ./app.jar
ENV SERVER_ADDRESS=0.0.0.0 \
    PORT=8080
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
