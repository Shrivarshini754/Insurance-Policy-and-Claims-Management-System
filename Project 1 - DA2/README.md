# InsureTrack — Insurance Policy Claims System (DA2)

## 1. Project Overview

InsureTrack is a Java-based Insurance Policy Claims Management System developed as part of DA2. The project uses **Oracle Database** to store and manage insurance-related information and a **Java Swing desktop application** to provide a user-friendly interface.

The system supports customer, policy, claim, payment, and asset management. It demonstrates database design, normalization, SQL and PL/SQL programming, JDBC connectivity, and application development.

## 2. Objectives

The main objectives of this project are to:

- Design and implement a relational database for an insurance management system.
- Convert the conceptual/EER design into relational tables.
- Apply database normalization to reduce redundancy.
- Maintain data integrity using primary keys, foreign keys, and constraints.
- Implement database operations using SQL and PL/SQL.
- Connect Java applications to Oracle Database using JDBC.
- Provide CRUD operations through a graphical interface.
- Integrate the frontend, backend, and database into one application.

## 3. Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Java |
| Frontend | Java Swing |
| Database | Oracle Database |
| Database Programming | SQL and PL/SQL |
| Connectivity | JDBC |
| JDBC Driver | Oracle `ojdbc17.jar` |
| Architecture | DAO and service layers |

## 4. Functional Modules

The application contains the following modules:

- **Customer Management:** View, add, search, update, and delete customer records.
- **Policy Management:** Manage insurance policies and their details.
- **Claim Management:** Register and maintain insurance claims.
- **Payment Management:** Record and manage payments associated with claims.
- **Asset Management:** Maintain information about insured assets.
- **Dashboard:** Display a summary of insurance-related information.
- **SQL Query Module:** Execute supported SQL queries and display their results.

## 5. Database Design

The database is designed using relational database concepts. The conceptual design is mapped into relational tables, with normalization used to organize data and reduce unnecessary duplication.

Key database concepts demonstrated include:

- Entity–Relationship and EER modelling.
- Relational mapping.
- First, Second, and Third Normal Forms (1NF, 2NF, and 3NF).
- Primary and foreign key constraints.
- Relationships between customers, policies, claims, payments, and assets.
- Referential integrity and data validation.

## 6. SQL and PL/SQL Implementation

**SQL** is used for database creation, insertion, retrieval, updating, deletion, searching, joins, and aggregate calculations.

**PL/SQL** supports procedural database operations, including stored procedures, functions, and triggers.

The project includes database-side operations such as:

- `add_customer` — Adds customer information.
- `register_claim` — Registers an insurance claim.
- `make_payment` — Records a payment.
- `get_claim_amount` — Retrieves the amount associated with a claim.
- `check_policy_expiry` — Validates policy expiry conditions.
- `display_claims` — Retrieves claim information.

The `insurance_setup.sql` file contains the database setup script for the project.

## 7. Application Architecture

The application follows a layered architecture that separates the user interface from database operations.

```text
Java Swing Frontend
        |
        v
BackendService
        |
        v
DAO Layer
        |
        v
DBConnection / JDBC
        |
        v
Oracle Database
```

- **Frontend Layer:** Provides forms, tables, navigation, and user interactions.
- **Service Layer:** Coordinates operations between the frontend and DAO classes.
- **DAO Layer:** Implements database operations for individual modules.
- **Connection Layer:** Establishes database connectivity through JDBC.
- **Database Layer:** Stores insurance information and enforces relational constraints.

## 8. Project Structure

```text
Project 1 - DA2/
├── src/
│   ├── dao/
│   ├── db/
│   ├── frontend/
│   ├── model/
│   └── service/
├── lib/
│   └── ojdbc17.jar
├── insurance_setup.sql
└── README.md
```

- `dao/` — Database access classes.
- `db/` — Oracle connection configuration.
- `frontend/` — Java Swing interface and application panels.
- `model/` — Classes for representing application data.
- `service/` — Application service logic.
- `lib/` — Oracle JDBC driver.
- `insurance_setup.sql` — Database setup script.

## 9. CRUD Operations

The application demonstrates the four fundamental database operations:

- **Create:** Add new records.
- **Read:** Retrieve and display records.
- **Update:** Modify existing records.
- **Delete:** Remove records according to the application's constraints.

These operations are available across the major application modules, subject to their validation and database rules.

## 10. Setup and Execution

### Prerequisites

Install:

- Java Development Kit (JDK).
- Oracle Database.
- An Oracle SQL client, such as SQL*Plus.
- Visual Studio Code or another Java IDE.

### Setup Steps

1. Clone or download the repository.
2. Open the `Project 1 - DA2` directory.
3. Start Oracle Database and configure the required database user.
4. Execute `insurance_setup.sql` using an Oracle SQL client.
5. Verify the connection settings in `DBConnection.java`.
6. Compile the Java source files with `ojdbc17.jar` on the classpath.
7. Run `frontend.MainFrame` to launch the desktop application.

**Security note:** Configure database credentials locally. Never commit real passwords or secrets to a public GitHub repository.

## 11. Testing

The application can be tested by verifying:

- Database connectivity.
- Table creation and constraints.
- SQL and PL/SQL operations.
- Record insertion, retrieval, updating, and deletion.
- Search and validation functionality.
- Claim and policy expiry validation.
- Frontend and backend integration.
- Display of database results in the application.

## 12. Conclusion

InsureTrack DA2 demonstrates the development of a database-driven Java desktop application using Oracle Database. The project combines relational database design, normalization, SQL and PL/SQL programming, JDBC connectivity, and CRUD operations in a single insurance management system.

It provides practical experience in applying DBMS concepts to a real-world-style application while integrating the database, backend, and frontend layers.

---

**Technology focus:** Java · Java Swing · Oracle Database · SQL · PL/SQL · JDBC
