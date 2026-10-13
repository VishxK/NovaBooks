# NovaBooks

NovaBooks is a Java-based bookstore web application built with Jakarta EE, Jersey, Hibernate, and embedded Tomcat. It supports both customer-facing shopping and admin management flows, including book catalog browsing, checkout, payment flow, order tracking, invoice generation, and admin dashboard analytics.

The project is designed as a full-stack MVC-style web application with REST APIs and static HTML views served from the `src/main/webapp` directory.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Setup and Installation](#setup-and-installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Endpoints](#api-endpoints)
- [Performance](#performance)
- [Security Notes](#security-notes)
- [Future Enhancements](#future-enhancements)

## Overview

NovaBooks is a complete online bookstore system for small-to-medium retail operations. It allows customers to:

- Browse books by keyword and price range
- View individual product pages
- Register and log in
- Maintain a session-based user profile
- Place and track orders
- Complete checkout and payment through PayHere
- Download invoices as PDF files

It also allows administrators to:

- Manage books and categories
- Upload book images
- Monitor customer accounts
- Update order status
- Review sales and order analytics on a dashboard

## Features

### Customer Features

- Book catalog browsing with pagination and search
- Filter by title, author, minimum and maximum price
- Product detail view by book ID
- User registration with email uniqueness validation
- Secure session-based login and logout
- Profile management for first name, last name, mobile number, and address
- Order history dashboard for authenticated users
- Order cancellation for pending orders only
- Checkout flow with shipping and billing details
- PayHere sandbox payment initiation and notification handling
- PDF invoice generation
- Dynamic order status and fulfillment tracking

### Admin Features

- Admin-only access for protected pages and actions
- Book management: add, update, delete, and view books
- Category creation and assignment
- Image upload support for book covers
- Customer management panel
- Admin dashboard with:
  - total books
  - total orders
  - total sales
  - total registered customers
  - monthly sales chart
  - order status summary
- Order status updates from admin panel

### System Features

- MySQL persistence via Hibernate ORM
- Embedded Tomcat server for local development
- Jersey REST API layer
- Multipart file upload for book images
- PDF generation for invoices using OpenPDF
- Mail integration using Jakarta Mail and Mailtrap-ready configuration
- Session-based authentication and authorization filters
- Lightweight JSON API responses using Gson

## Technology Stack

- Java 17
- Maven
- Hibernate ORM 6
- Jersey / JAX-RS 3
- Tomcat Embedded 10
- MySQL Connector/J
- Jakarta Servlet API
- Gson
- OpenPDF
- Jakarta Mail
- HTML/CSS/JavaScript for frontend views

## Project Structure

```text
NovaBooks/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── lk/vish/novabooks/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── entity/
│   │   │       ├── filter/
│   │   │       ├── listener/
│   │   │       ├── mail/
│   │   │       ├── middleware/
│   │   │       ├── provider/
│   │   │       ├── Service/
│   │   │       ├── util/
│   │   │       └── Main.java
│   │   ├── resources/
│   │   │   ├── app.properties
│   │   │   └── hibernate.cfg.xml
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       ├── *.html
│   │       └── images/
│   └── test/
├── pom.xml
├── .gitignore
├── NovaBooks.iml
└── README.md
```

## Architecture

The application follows a layered architecture:

- Presentation layer: static HTML pages and JavaScript-driven frontend interactions
- API layer: Jersey resource classes under `controller/api`
- Service/data layer: Hibernate entity persistence and DAO-like session usage
- Middleware layer: session checks and access filters
- Infrastructure: email, PDF, and payment integration utilities

The embedded Tomcat server is configured in `Main.java` and exposes the API under `/api/*`, while static pages are served from the `webapp` directory.

## Setup and Installation

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- IDE such as IntelliJ IDEA (recommended)

### 1) Clone the repository

```bash
git clone https://github.com/VishxK/NovaBooks.git
cd NovaBooks
```

### 2) Create the database

Open MySQL and create a database:

```sql
CREATE DATABASE novabooks;
```

### 3) Update database credentials

Edit:

- `src/main/resources/hibernate.cfg.xml`

Set your database URL, username, and password:

```xml
<property name="hibernate.connection.url">jdbc:mysql://localhost:3306/novabooks?useSSL=false&amp;allowPublicKeyRetrieval=true</property>
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">your_password</property>
```

### 4) Update app configuration

Edit `src/main/resources/app.properties` and set your preferred values for:

- mail host / port / username / password
- app name and base URL
- PayHere merchant credentials

### 5) Build the project

```bash
mvn clean package
```

### 6) Run the application

From IntelliJ IDEA:

- Open `Main.java`
- Run the `main` method

The app runs on:

```text
http://localhost:8080/novabooks
```

## Configuration

### Admin credentials

The application includes a built-in admin login:

- Email: `admin@novabooks.lk`
- Password: `admin123`

### Payment configuration

The project uses PayHere sandbox configuration via `app.properties` and the `PayHereHash` utility. Replace the merchant credentials before production deployment.

### Email configuration

Email sending is configured through Mailtrap-ready SMTP properties. Update the values in `app.properties` before enabling real outbound email.

## Usage

### Customer workflow

1. Open the bookstore home page.
2. Browse or search books.
3. Open a product detail page.
4. Add books to cart or proceed to checkout.
5. Register or sign in.
6. Complete delivery info.
7. Pay using the PayHere integration.
8. View order history and invoice.

### Admin workflow

1. Log in using the admin account.
2. Open the admin dashboard.
3. Add or update books and categories.
4. Upload book images.
5. View sales and order analytics.
6. Update order status and customer state.

## API Endpoints

### Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/auth/check-session`
- `POST /api/auth/logout`
- `PUT /api/auth/profile`
- `POST /api/auth/change-password`

### Books

- `GET /api/books`
- `GET /api/books/{id}`
- `POST /api/books`

### Orders

- `GET /api/orders/list`
- `POST /api/orders/cancel/{orderId}`

### Admin Book Management

- `GET /api/admin/books`
- `POST /api/admin/books`
- `GET /api/admin/categories`
- `POST /api/admin/books/save`
- `GET /api/admin/books/get?id={id}`
- `DELETE /api/admin/books/{id}`

### Admin Dashboard

- `GET /api/admin/dashboard/stats`

### Admin Orders

- `GET /api/admin/orders/list`
- `POST /api/admin/orders/update-status`

### Payment

- `POST /api/payment/initiate`
- `POST /api/payment/notify`

## Performance

NovaBooks is designed to deliver strong performance for a bookstore workload without unnecessary complexity.

### Performance characteristics

| Area | Optimization | Benefit |
|------|--------------|---------|
| Catalog browsing | Pagination and query-based filtering | Reduces large result sets and database load |
| Dashboard analytics | Aggregate `COUNT` and `SUM` queries | Fast summary reporting for sales and order metrics |
| Order retrieval | Hibernate query with fetch joins | Minimizes N+1 access when loading orders, items, and books |
| Image handling | Multipart upload with size limits | Prevents oversized payloads and improves server stability |
| Session managment | HTTP session with configurable timeout | Efficient user state tracking without repeated authentication checks |
| Database access | Hibernate connection pooling (`pool_size` = 20) | Better concurrency under moderate traffic |

### Performance profile

- Book listing is page-based rather than loading the entire catalog at once.
- Dashboard statistics use aggregate database queries instead of loading all rows into memory.
- Product and order retrieval are optimized by fetching related entities in controlled batches.
- The application is lightweight enough to run locally on a typical developer machine and scale to moderate production traffic.

### Benchmarking note

This repository does not currently include a dedicated benchmark suite or production load test. The project’s performance characteristics are therefore based on the implemented architecture and optimization choices rather than a formal benchmarking harness.

## Security Notes

This project is a functional learning/demo application and should be hardened before production use.

Recommended improvements:

- Replace hardcoded admin credentials with environment variables or a secure secrets manager
- Use password hashing (BCrypt/Argon2) instead of plain text password comparison
- Add CSRF protection for state-changing actions
- Validate all incoming API payloads more strictly
- Restrict admin-only endpoints to proper role checks
- Move secret keys and SMTP credentials out of source-controlled files
- Use HTTPS in production

## Future Enhancements

- Product rating and review system
- Shopping cart persistence in database or browser storage
- Advanced analytics and reporting
- Inventory stock alerts
- Refund and return workflow
- Role-based access using JWT or OAuth
- Better frontend framework integration (React/Vue)
- Docker and CI/CD setup

## License

This project is provided as an educational and demo application. Please check the repository license file if one is added later.

## Contributing

Contributions are welcome. If you want to improve the design, performance, or feature set, open a pull request with a clear explanation of the changes.

## Acknowledgements

- Hibernate and Jersey for backend simplification
- Apache Tomcat for embedded deployment
- OpenPDF for invoice generation
- PayHere for payment integration
- Java and Jakarta EE ecosystem for web application development
