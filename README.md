# Movie Info Service

A Spring Boot REST API that retrieves movie information (title, year, directors) from two external sources: [OMDB API](http://www.omdbapi.com/) and [The Movie Database (TMDB)](https://www.themoviedb.org/documentation/api).

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose

---

## Configuration

Before running the application, set your API keys in `src/main/resources/application.yml`:

```yaml
movie:
  api:
    omdb:
      api-key: YOUR_OMDB_API_KEY       # https://www.omdbapi.com
    tmdb:
      api-key: YOUR_TMDB_API_KEY       # https://www.themoviedb.org
```

---

## Starting the infrastructure

MySQL and Redis are provided via Docker Compose. From the project root:

```bash
# Start MySQL and Redis in the background
docker compose up -d

# Check that both containers are healthy
docker compose ps

# Stop containers (data is preserved in volumes)
docker compose down

# Stop containers and delete all data
docker compose down -v
```

---

## Running the application

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

---

## API usage

```
GET /movies/{movieTitle}?api={apiName}
```

| Parameter    | Required | Values        |
|--------------|----------|---------------|
| `movieTitle` | yes      | any string    |
| `api`        | yes      | `omdb`, `tmdb`|

**Example requests:**

```bash
curl "http://localhost:8080/movies/Inception?api=omdb"
curl "http://localhost:8080/movies/Inception?api=tmdb"
```

**Example response:**

```json
{
  "movies": [
    {
      "Title": "Inception",
      "Year": "2010",
      "Director": ["Christopher Nolan"]
    }
  ]
}
```

---

## Running the tests

Tests use an in-memory H2 database and simple cache — no Docker required.

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=OmdbApiAdapterTest

# Run tests with detailed output
mvn test -Dsurefire.failIfNoSpecifiedTests=false
```

---

## Project structure

```
src/
├── main/java/movieinfo/
│   ├── controller/        # REST endpoint
│   ├── service/
│   │   ├── external/      # OMDB and TMDB adapter implementations
│   │   ├── CacheService
│   │   ├── MovieService
│   │   └── SearchLogService
│   ├── exception/         # Global error handling
│   └── entity/            # SearchLog JPA entity
└── test/java/movieinfo/
    ├── controller/        # MockMvc controller tests
    └── service/           # Unit tests with MockWebServer and Mockito
```
