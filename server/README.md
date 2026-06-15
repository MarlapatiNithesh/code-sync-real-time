# CodeSync Backend Server

This is the Java Spring Boot backend server for CodeSync, a real-time collaborative code-editing and synchronization application. It exposes REST APIs for authentication/room management and a Netty-based Socket.io server for real-time collaboration signaling.

---

## Technical Stack

* **Core Framework:** Java 21 & Spring Boot 3
* **Database Management:** Hibernate JPA & Spring Data JPA
* **Real-time Communication:** Netty Socket.io
* **Cache & Pub/Sub System:** Spring Data Redis
* **Security:** Spring Security & JWT Token Authentication
* **Build System:** Maven

---

## Configuration (Environment Variables)

The backend server is configured via properties in `application.properties` and can be customized at runtime using environment variables:

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `API_PORT` | `8080` | Tomcat HTTP REST API port |
| `PORT` | `3000` | Netty Socket.io WebSocket port |
| `CLIENT_URL` | `*` | Allowed CORS client origin URL |
| `BACKEND_URL` | `http://localhost:8080` | Target self URL configuration |
| `DB_URL` | `jdbc:mysql://localhost:3306/codesync...` | JDBC database connection string |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | `nitheesh` | Database password |
| `REDIS_HOST` | `localhost` | Redis caching instance hostname |
| `REDIS_PORT` | `6379` | Redis caching instance port |
| `JWT_SECRET` | `code-sync-jwt-secret-change...` | JWT secret used to sign secure session tokens |

---

## Local Setup Instructions

### Prerequisites
1. **Java Development Kit (JDK) 21** or higher installed.
2. **Maven** installed.
3. Running **MySQL** service on port `3306` (or `3307`).
4. Running **Redis** service on port `6379`.

### Build & Run
1. Navigate to the server directory:
   ```bash
   cd server
   ```
2. Build the application:
   ```bash
   mvn clean package -DskipTests
   ```
3. Run the application locally:
   ```bash
   mvn spring-boot:run
   ```

---

## Docker Deployment

When deployed using Docker, configuration environment variables are mapped inside `docker-compose.yml`. The application builds using the multi-stage `Dockerfile` and is served as a minimal Alpine JRE runner image.
