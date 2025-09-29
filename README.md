# Prueba Técnica – API REST (Quarkus)

**Objetivo:** API que consume JSONPlaceholder y devuelve *posts* combinados con su autor y comentarios.

---

## Stack
- Java 17
- Quarkus 3 (RESTEasy Reactive + Jackson)
- Java `HttpClient` asíncrono (`CompletableFuture`)
- Bean Validation (validación de parámetros)
- JUnit + RestAssured (tests)
- WireMock (tests de integración)
- OpenAPI/Swagger UI
- Dockerfile + GitHub Actions (CI)

---

## Endpoints principales

### `GET /posts` (v1)
Fusiona datos de múltiples servicios externos.

**Query params opcionales:**
- `limit` (1..100): limita la cantidad de posts
- `includeComments` (true/false, default = **true**)
- `userId` (int): filtra por autor

**Ejemplo de respuesta (recortado):**
```json
[
  {
    "id": 1,
    "title": "sunt aut facere...",
    "body": "...",
    "author": { "id": 1, "name": "Leanne Graham", "email": "..." },
    "commentCount": 5,
    "comments": [{ "id": 1, "email": "..." }]
  }
]
```

**Códigos posibles:**
- `200 OK` → correcto
- `502 Bad Gateway` → falla la API externa (capturado por `GlobalExceptionMapper`)

---

### `GET /v2/posts`
Versión avanzada con **paginación, búsqueda y ordenamiento**.

**Query params:**
- `page` (default 1)
- `size` (default 20, máximo 100)
- `q` → búsqueda en `title`, `body`, `author.name`
- `sortBy` → `id | title | author | commentCount`
- `order` → `asc | desc`
- `userId`
- `includeComments` (default true)

**Headers devueltos:**
- `X-Total-Count` (total registros)
- `X-Page`, `X-Size`

---

### Otros endpoints V2
- `GET /v2/posts/{id}` → detalle por ID  
- `GET /v2/posts/users/{userId}` → posts de un usuario  
- `GET /v2/posts/stats` → estadísticas (total, posts por user, máximo y promedio de comentarios)

---

## Cómo correrlo

### Requisitos
- Java 17
- Maven 3.9+

### Ejecutar en modo desarrollo
```bash
mvn quarkus:dev
```

### Compilar
```bash
mvn package
```

### Probar con curl
```bash
curl -H "X-API-Key: change-me" "http://localhost:8080/posts?limit=5&includeComments=true"
curl -H "X-API-Key: change-me" "http://localhost:8080/v2/posts?q=sunt&sortBy=commentCount&order=desc&page=1&size=10"
curl -H "X-API-Key: change-me" "http://localhost:8080/v2/posts/stats"
```

---

## Observabilidad
- **Swagger UI:** [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui)  
- **Health checks:** [http://localhost:8080/health](http://localhost:8080/health)  
- **Metrics (Prometheus):** [http://localhost:8080/q/metrics](http://localhost:8080/q/metrics)  

---

## Seguridad
- **API Key opcional**: header `X-API-Key`  
- Configuración: `security.api-key=change-me` en `application.properties`  
- **Rate limiting**: 60 req/min por IP (configurable)

---

## Estructura del proyecto
```
src/main/java/com/example
├── model
│   ├── Post.java
│   ├── Comment.java
│   ├── User.java
│   └── MergedPost.java
├── service
│   └── PostService.java
├── rest
│   ├── PostsResource.java
│   ├── PostsResourceV2.java
│   └── GlobalExceptionMapper.java
├── filters
│   ├── ApiKeyFilter.java
│   ├── RateLimitFilter.java
│   └── LoggingFilter.java
└── health
    └── HealthChecks.java
```

---

## Tests
- **Unit/Smoke:** `PostsResourceTest`
- **Integración (WireMock):** `PostsIntegrationTest` (stubs de `/posts`, `/users/1`, `/posts/1/comments`)

Ejecutar:
```bash
mvn test
```

---

## Docker
Build:
```bash
docker build -t bank-challenge:latest .
```

Run:
```bash
docker run -p 8080:8080 -e "SECURITY_API_KEY=change-me" bank-challenge:latest
```

---

## CI/CD
- Workflow GitHub Actions: `.github/workflows/ci.yml`  
- Corre build + tests automáticamente en cada push/PR  

---

## Extras implementados
- OpenAPI + Swagger UI
- Health checks & métricas
- Fault tolerance: retries, timeouts, circuit breaker
- Cache de usuarios (`@CacheResult`)
- Rate limiting (token bucket)
- API Key opcional
- Logging de requests/responses
- Endpoints V2: búsqueda, paginación, orden
- Postman Collection lista para importar
- Dockerfile multi-stage
- CI con GitHub Actions
- WireMock para tests determinísticos
