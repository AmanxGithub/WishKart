# WishKart Project Guidelines

## Project Overview

**WishKart** is a full-featured e-commerce platform built with Spring Boot 3.2.2. It provides a complete ecosystem for product browsing, wishlist management, checkout, and payment processing.

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.2
- **Build Tool**: Maven
- **Status**: Active Development (1.0.0-SNAPSHOT)
- **Repository**: Git

## Technology Stack

### Core Spring Boot Dependencies
- **spring-boot-starter-web**: REST API and web services
- **spring-boot-starter-data-jpa**: Database ORM and entity management
- **spring-boot-starter-security**: Authentication and authorization
- **spring-boot-starter-validation**: Input validation with JSR-303/JSR-380
- **spring-boot-starter-thymeleaf**: Server-side templating (if HTML UI used)
- **spring-boot-starter-mail**: Email notifications
- **spring-boot-starter-actuator**: Metrics, health checks, monitoring
- **spring-boot-devtools**: Hot-reload during development (runtime scope, optional)

### Additional Libraries
- **JWT Authentication**: JJWT 0.12.3 (jjwt-api, jjwt-impl, jjwt-jackson)
- **Payment Processing**: Stripe Java SDK 24.15.0
- **Object Mapping**: MapStruct 1.5.5.Final (with processor plugin)
- **Boilerplate Reduction**: Lombok (optional, with mapstruct binding)
- **Documentation**: Springdoc OpenAPI 2.3.0 (Swagger UI integration)
- **Message Broker**: Spring Kafka (spring-kafka)
- **Template Engine**: Thymeleaf Extras for Spring Security 6

### Database
- **H2 Database**: File-based, embedded SQL database (runtime scope)
- **Dialect**: JPA auto-configures for H2 in Spring Data
- **Storage**: `data/` directory (check `application.properties` for path configuration)

### Testing
- **spring-boot-starter-test**: JUnit 5, AssertJ, Mockito
- **spring-security-test**: Security-aware test utilities

## Project Structure

```
WishKart/
├── src/
│   ├── main/
│   │   ├── java/com/wishkart/
│   │   │   ├── controller/        # REST API endpoints
│   │   │   ├── service/           # Business logic
│   │   │   ├── repository/        # Data access layer (Spring Data JPA)
│   │   │   ├── entity/            # JPA entities
│   │   │   ├── dto/               # Data Transfer Objects
│   │   │   ├── security/          # JWT, authentication, authorization
│   │   │   ├── mapper/            # MapStruct mappers
│   │   │   ├── config/            # Spring configuration classes
│   │   │   ├── exception/         # Global exception handling
│   │   │   ├── event/             # Kafka event producers/consumers
│   │   │   └── WishKartApplication.java
│   │   └── resources/
│   │       ├── application.properties    # Main configuration
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── templates/                # Thymeleaf templates (if used)
│   └── test/
│       ├── java/com/wishkart/
│       │   ├── controller/        # Controller tests
│       │   ├── service/           # Service layer tests
│       │   └── repository/        # Repository tests
│       └── resources/
│           └── application-test.properties
├── docker/                        # Docker configuration
├── logs/                          # Application logs (gitignored)
├── data/                          # H2 database file storage (gitignored)
├── pom.xml                        # Maven configuration
├── AGENTS.md                      # This file
└── README.md                      # Project documentation
```

## Development Workflow

### Build & Compile
```bash
# Clean build
mvn clean compile

# Full build with tests
mvn clean package

# Skip tests during build
mvn clean package -DskipTests

# Install to local Maven repository
mvn clean install
```

### Running the Application

#### Via Maven
```bash
# Development mode (auto-reload enabled)
mvn spring-boot:run

# Production mode
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

#### Via IDE (IntelliJ IDEA)
1. Create a Spring Boot run configuration
2. **Before Launch**: Ensure "Build" is configured
3. **VM Options**: `-Dspring-boot.devtools.restart.enabled=true` (for hot-reload)
4. Enable "Build project automatically" in Settings → Build → Compiler
5. Enable "Allow auto-make to start..." in Settings → Advanced Settings

#### Debugging
- Use the "Debug" button in IntelliJ to start debugger
- DevTools will auto-restart on class changes
- If target folder is stale: Run `mvn clean compile` to refresh

### Testing

#### Unit Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn test jacoco:report
```

#### Integration Tests
- Place in `src/test/java/com/wishkart/` with `*IT.java` naming
- Use `@SpringBootTest` for full Spring context
- Use `@DataJpaTest` for repository tests
- Use `@WebMvcTest` for controller tests with mocked services

#### Security Tests
- Import `spring-security-test` for `@WithMockUser`, `@WithAnonymousUser`
- Test authentication flows with mock JWT tokens

### Database Management

#### H2 Console (Development)
- Enable in `application-dev.properties`:
  ```properties
  spring.h2.console.enabled=true
  spring.h2.console.path=/h2-console
  ```
- Access at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/wishkart` (match `application.properties` path)

#### Schema & Migrations
- JPA auto-creates tables from entities (check `spring.jpa.hibernate.ddl-auto`)
- For migrations, consider adding Flyway or Liquibase in production
- Entity classes define schema structure (see `entity/` package)

## Architecture & Design Patterns

### Layered Architecture
```
Controller → Service → Repository → Entity/Database
                ↓
           Exception Handler
```

### Key Patterns
1. **Repository Pattern**: Spring Data JPA repositories handle data access
2. **Service Layer**: Business logic encapsulated in `@Service` beans
3. **DTO Pattern**: MapStruct mappers convert entities ↔ DTOs
4. **Security**: JWT-based stateless authentication via filters
5. **Event-Driven**: Kafka topics for async messaging (order events, notifications, etc.)
6. **Global Exception Handling**: `@RestControllerAdvice` for consistent error responses

### Security Architecture
- **JWT Token**: Signed with JJWT, stored client-side
- **Stateless**: No session storage, scalable horizontally
- **Authorities/Roles**: Spring Security for role-based access control (RBAC)
- **Password Encoding**: BCrypt (configured via `SecurityConfig`)

### Payment Integration
- **Stripe API**: Used for payment processing
- **Webhook Handling**: Listen for Stripe events (webhooks at `/webhooks/stripe`)
- **Idempotency**: Implement idempotency keys for Stripe API calls

## Best Practices for AI Assistants

### Code Style & Conventions
1. **Naming**: 
   - Classes: `PascalCase` (e.g., `UserService`, `OrderController`)
   - Methods: `camelCase` (e.g., `getUserById`, `createOrder`)
   - Constants: `UPPER_SNAKE_CASE`
   - Package names: lowercase, reverse domain notation (e.g., `com.wishkart.controller`)

2. **Annotations**:
   - Use Lombok: `@Data`, `@Getter`, `@Setter`, `@AllArgsConstructor`
   - Use MapStruct: `@Mapper`, `@Mapping` for DTOs
   - Use Spring: `@Service`, `@Repository`, `@RestController`, `@Component`

3. **Exception Handling**:
   - Create custom exceptions extending `RuntimeException` or `Exception`
   - Throw with descriptive messages
   - Catch and wrap in `@RestControllerAdvice` handlers
   - Example: `ResourceNotFoundException`, `BadRequestException`

### Writing Tests
- Use `@SpringBootTest` for integration tests, `@WebMvcTest` for controller tests
- Mock external dependencies (Stripe, Kafka) with Mockito
- Prefix tests with test data setup (use factories or `@BeforeEach`)
- Assert both success and failure paths
- Test security: `@WithMockUser`, `@WithAnonymousUser`, `WithSecurityContextFactory`

### Working with Entities & DTOs
- **Entities**: Map to database tables, use `@Entity`, `@Table`, `@Id`, JPA annotations
- **DTOs**: Lightweight objects for API responses, use MapStruct mappers
- **Relationships**: Use lazy loading (`FetchType.LAZY`) by default, avoid N+1 queries
- **Validation**: Use `@Valid` on DTO fields with JSR-303 annotations (`@NotNull`, `@Min`, etc.)

### API Design
- Use RESTful conventions: `GET /api/products`, `POST /api/orders`, `PUT /api/products/{id}`
- Return consistent JSON responses with proper HTTP status codes
- Use OpenAPI/Swagger annotations: `@Operation`, `@ApiResponse`, `@Parameter`
- Validate input with `@Valid` and custom `@CrossOrigin` as needed

### Kafka Messaging
- Producers: Inject `KafkaTemplate<String, Object>`, send messages asynchronously
- Consumers: Use `@KafkaListener` for topic subscriptions
- Event classes: Create POJOs in `event/` package, ensure serializability
- Error handling: Implement `ErrorHandler` for failed message processing

### Common File Locations
- **Configuration**: `src/main/java/com/wishkart/config/`
- **Global Exception Handler**: `src/main/java/com/wishkart/exception/GlobalExceptionHandler.java`
- **Security Config**: `src/main/java/com/wishkart/security/SecurityConfig.java`
- **Properties**: `src/main/resources/application.properties` (base), `application-{profile}.properties` (profiles)

## Debugging & Troubleshooting

### Issue: Target folder not updating during debug
**Solution**:
1. Ensure "Build" is in run configuration's "Before Launch" steps
2. Enable "Build project automatically" in IDE settings
3. Run `mvn clean compile` to refresh target/
4. Add `spring-boot-devtools` (already in pom.xml)

### Issue: H2 data not persisting
**Check**:
- H2 file path in `application.properties`: `spring.datasource.url=jdbc:h2:file:./data/wishkart`
- Ensure `data/` directory exists and is writable
- Shutdown application cleanly to flush data to disk

### Issue: JWT token invalid
**Check**:
- Token not expired (`exp` claim)
- Signature valid (secret key matches)
- Token format: `Bearer {token}` in `Authorization` header
- Check `SecurityConfig` for token validation logic

### Issue: Kafka messages not being consumed
**Check**:
- Kafka broker running and accessible
- Consumer group ID configured in `@KafkaListener(groupId = "...")`
- Topic exists or auto-created setting enabled
- Check logs for consumer lag or rebalancing issues

## Commit Message Convention

Use conventional commits with Jira tickets (if applicable):

```
[TICKET-123] feat: add user authentication endpoint

- Implement JWT token generation
- Add SecurityConfig for OAuth2 integration
- Include unit tests for authentication flow

Co-Authored-By: wibey jetbrains plugin (wibey.walmart.com/code)
```

Or without ticket:
```
feat: add product filtering by category
fix: resolve N+1 query issue in order service
docs: update API documentation for checkout endpoint
```

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [JJWT Library](https://github.com/jwtk/jjwt)
- [Stripe Java SDK](https://github.com/stripe/stripe-java)
- [MapStruct](https://mapstruct.org/)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [OpenAPI / Swagger](https://springdoc.org/)
