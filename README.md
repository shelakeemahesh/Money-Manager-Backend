# ☕ Money Manager — Backend API Service

A highly optimized, production-ready REST API built with **Spring Boot 3**, **Java 21**, and **Spring Security (JWT)**. It uses **MySQL** (via Spring Data JPA) to handle data storage with transaction safety and database indexing.

---

## 🚀 Key Features

* **Secure Authentication**: Uses Stateless JWT authentication. JWT configurations validate at startup (fail-fast checks for key length & expiration).
* **Role-Based Access Control**: Standard users and admins are segregated via method-level security `@PreAuthorize`.
* **Automated Seed/Defaults**: Automatically sets up default categories (`Salary`, `Food`, `Transport`, etc.) upon registration using isolation propagation rules.
* **Audit Logging**: Comprehensive admin action logging, tracing operations, IP addresses, and targets.
* **Optimized Database Queries**: Solves Hibernate N+1 problems using explicit `JOIN FETCH` queries for paginated views.
* **Database Indexes**: Custom indexing on tables (`tbl_expenses`, `tbl_incomes`, `tbl_profiles`, `tbl_admin_audit_logs`) to ensure swift performance for query scaling.
* **Docker Ready**: Minimal Docker footprint utilizing multi-stage builds and a JRE runtime (~50% smaller container size).

---

## 🛠️ Tech Stack & Dependencies

* **Core**: Spring Boot 3.4.5, Java 21, Maven.
* **Security**: Spring Security, JJWT (Javassist JSON Web Token) v0.11.5.
* **Database**: Hibernate/JPA, MySQL Connector J (`com.mysql:mysql-connector-j`), HikariCP Connection Pool.
* **Mailing**: Spring Boot Starter Mail (configured for Brevo SMTP or Gmail).
* **Utilities**: Lombok, Apache POI (for future Excel creation / exports).

---

## 💻 Local Setup & Development

### 1. Prerequisites
* **Java JDK 21** installed.
* **Maven** installed (or use the provided wrapper `./mvnw`).
* A running **MySQL** server.

### 2. Configure Database & Environment
Copy `.env.example` to `.env` (or configure your shell's environment variables).
Ensure you have created a local database named `moneymanager`.

### 3. Run Locally
Use the Spring Boot Maven plugin:
```bash
./mvnw spring-boot:run
```
The API will run at `http://localhost:8080/api/v1.0`.

---

## 🐳 Docker Support

The codebase includes a fully optimized multi-stage `Dockerfile`.

### Build Image
```bash
docker build -t money-manager-backend .
```

### Run Container
```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/moneymanager \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e APP_JWT_SECRET=your-secure-random-secret-key-of-at-least-32-chars \
  --name money-manager-api \
  money-manager-backend
```

---

## ☁️ Deployment on Render

Render is set to use the Docker runtime for the backend container.

### Step-by-Step Render Setup:
1. Push this backend directory to its own GitHub repository.
2. Go to the Render Dashboard -> **New** -> **Web Service**.
3. Connect your GitHub repository.
4. Set the following options:
   * **Runtime**: `Docker`
   * **Branch**: `main`
5. In **Advanced**, add the following **Environment Variables**:
   * `SPRING_DATASOURCE_URL`: `jdbc:mysql://<railway-host>:<railway-port>/railway?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` (retrieve from Railway console)
   * `SPRING_DATASOURCE_USERNAME`: `<railway-username>`
   * `SPRING_DATASOURCE_PASSWORD`: `<railway-password>`
   * `APP_JWT_SECRET`: `your-secure-random-secret-key-of-at-least-32-chars`
   * `APP_CORS_ALLOWED_ORIGINS`: `https://your-frontend.vercel.app` (your deployed Vercel frontend URL)
   * `APP_BASE_URL`: `https://your-backend.onrender.com/api/v1.0` (your Render URL)
   * `BREVO_USERNAME`: `your-smtp-username`
   * `BREVO_PASSWORD`: `your-smtp-api-key`
6. Click **Deploy Web Service**.

---

## 📝 API Endpoints Summary

### Public / Auth Endpoints:
* `POST /api/v1.0/register` — Create a new profile. Sends activation link.
* `POST /api/v1.0/login` — Sign in and receive JWT token.
* `GET /api/v1.0/activate?token=...` — Activate account.
* `POST /api/v1.0/forgot-password` — Request password reset email.
* `GET /api/v1.0/health` or `/status` — Public health checks.

### Private / Authenticated Endpoints:
*(Requires header `Authorization: Bearer <your_jwt_token>`)*

#### Incomes
* `GET /api/v1.0/incomes` — List all user's incomes (sorted).
* `POST /api/v1.0/incomes` — Add income.
* `DELETE /api/v1.0/incomes/{id}` — Delete income.

#### Expenses
* `GET /api/v1.0/expenses` — List all user's expenses.
* `POST /api/v1.0/expenses` — Add expense.
* `DELETE /api/v1.0/expenses/{id}` — Delete expense.

#### Categories
* `GET /api/v1.0/categories` — List user's categories (with fetched subcategories).
* `POST /api/v1.0/categories` — Add a new category.
* `POST /api/v1.0/categories/{id}/subcategories` — Add subcategory.

#### Admin Board
*(Requires role `ADMIN`)*
* `GET /api/v1.0/admin/users` — List and filter users.
* `PUT /api/v1.0/admin/users/{id}/status` — Update user status (`ACTIVE`, `BANNED`, `SUSPENDED`).
* `GET /api/v1.0/admin/audit-logs` — Paginated list of administrative action records (optimized with `JOIN FETCH`).
* `GET /api/v1.0/admin/stats` — Overall statistics of total volume, system loads, and user status distributions.
