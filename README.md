# Secure E-Commerce Web Application

This is a **secure e-commerce web application** developed for **Assessment 2: Secure Web Application Development**.  
The application implements **OIDC authentication (via Asgardeo)**, **JWT-based authorization**, **OWASP Top 10 security measures**, and **comprehensive order management functionality**.

---

## Features

### Authentication & Security
- **OIDC (OpenID Connect) Authentication** using Asgardeo
- **JWT Token Management** for stateless authentication
- **OWASP Top 10 Security Measures** implemented:
  - Broken Access Control
  - Cryptographic Failures
  - Injection Prevention
  - Insecure Design Mitigation
  - Security Misconfiguration Prevention
  - Vulnerable Components Management
  - Authentication Failures Prevention
  - Software and Data Integrity
  - Security Logging & Monitoring
  - Server-Side Request Forgery (SSRF) Prevention
- **HTTPS support** for production deployment
- **Rate Limiting** to prevent abuse
- **Input validation and sanitization** (XSS/SQL injection prevention)
- **Access control based on user ownership**

### User Management
- Display **user profile**: username, name, email, contact number, country
- Profile **editing capabilities**
- Secure authentication flow with JWT & OIDC

### Order Management
- Create orders with business rules:
  - Purchase date (must be today or future, excluding Sundays)
  - Delivery time 
  - Delivery location 
  - Product selection from predefined catalog
  - Quantity (1–10)
  - Optional message
- View order history
- Order status tracking (Pending, Confirmed, Shipped, Delivered)
- Order cancellation (pending orders only)

---

## Technology Stack

**Backend**
- Spring Boot (Java)
- JWT authentication
- Asgardeo OAuth / OIDC login
- Spring Security for CSRF, CORS, and headers
- Lombok for boilerplate reduction
- Hibernate / JPA with MySQL or SQLite
- OWASP HTML Sanitizer for XSS protection

**Frontend**
- React.js with Vite
- Redux Toolkit for state management
- React Router for navigation
- Tailwind CSS for styling
- React Hook Form for form handling
- DOMPurify for frontend XSS protection

---

## Security Features (OWASP Top 10 Mitigation)

| Vulnerability                     | Implementation in Project                                                                 |
|----------------------------------|------------------------------------------------------------------------------------------|
| **Broken Access Control**         | Ownership-based access, role-based access, JWT claims                                     |
| **Cryptographic Failures**        | HTTPS enforcement, secure JWT, password hashing with BCrypt                               |
| **Injection**                     | Input validation, sanitization, parameterized queries                                     |
| **Insecure Design**               | Security-first architecture, fail-secure defaults                                         |
| **Security Misconfiguration**     | Proper headers, CORS configuration, error page suppression                                 |
| **Vulnerable Components**         | Regular dependency updates, OWASP dependency checks                                       |
| **Authentication Failures**       | OIDC login, JWT token expiry, secure password storage                                      |
| **Software & Data Integrity**     | Input validation, output sanitization                                                    |
| **Security Logging**              | Logging of failed logins, admin actions, and order modifications                           |
| **Server-Side Request Forgery**   | Proper CORS configuration, input validation                                              |

---

## Installation & Setup

### Prerequisites
- Java 17+  
- Maven  
- Node.js 16+  
- MySQL / SQLite database  
- Asgardeo account (for OIDC login)

---

### Backend Setup
1. Clone the repository:
```bash
git clone <repository-url>
cd shopBackend
Install dependencies:

bash
Copy code
mvn clean install
Configure environment variables in application.properties (or .env for Spring Boot externalized config):

properties
Copy code
# Server
server.port=8443
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=yourpassword
server.ssl.keyStoreType=PKCS12
server.ssl.keyAlias=tomcat

# JWT
jwt.secret=your_jwt_secret
jwt.expiration=3600000

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/fuelstation
spring.datasource.username=root
spring.datasource.password=yourpassword

# OIDC / Asgardeo
asgardeo.clientId=YOUR_CLIENT_ID
asgardeo.clientSecret=YOUR_CLIENT_SECRET
asgardeo.redirectUri=http://localhost:3000/profile
asgardeo.issuer=https://api.asgardeo.io/t/yourtenant

# Security
server.error.whitelabel.enabled=false
Run the backend:

bash
Copy code
mvn spring-boot:run


Asgardeo Configuration
Register an application in Asgardeo Console (Traditional Web Application)

Set Authorized Redirect URL to: http://localhost:3000/profile

Set Allowed Origins: http://localhost:3000

Use the client ID and secret in backend .properties file

Usage
Navigate to http://localhost:3000

Register a new account or login via Asgardeo

Create and manage orders while respecting business rules

Admin users can manage products and categories

Security Practices
CSRF tokens enforced for all state-changing requests

Input sanitization both on frontend (DOMPurify) and backend (OWASP HTML Sanitizer)

CORS restricted to trusted frontend URL

HTTPS enforced in production

Rate limiting for APIs to prevent abuse
