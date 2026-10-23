# InsureTrack — Insurance Policy Claims System (DA2)

## Overview
InsureTrack is a Java-based Insurance Policy Claims Management System developed using Oracle Database and Java Swing. It manages customers, policies, claims, payments, and insured assets.

## Technologies Used
- **Language:** Java
- **Frontend:** Java Swing
- **Database:** Oracle Database
- **Database Programming:** SQL and PL/SQL
- **Connectivity:** JDBC
- **Architecture:** DAO and service layers

## Features
- Customer, policy, claim, payment, and asset management
- CRUD operations (Create, Read, Update, Delete)
- Search and record management
- Dashboard statistics
- SQL query execution
- Database validation and integrity constraints

## Database Concepts
- EER and relational database design
- Normalization
- Primary and foreign keys
- SQL queries and joins
- PL/SQL procedures, functions, and triggers
- JDBC database connectivity

## Project Structure
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

## Setup
1. Install Java JDK and Oracle Database.
2. Configure the Oracle connection in `DBConnection.java`.
3. Run `insurance_setup.sql` using an Oracle SQL client.
4. Compile the Java source files with `ojdbc17.jar` on the classpath.
5. Run `frontend.MainFrame` to launch the application.

**Note:** Configure database credentials locally. Do not commit passwords or secrets to GitHub.

## Purpose
This project demonstrates relational database design, normalization, SQL/PL/SQL programming, JDBC integration, and the development of a database-driven Java desktop application.
