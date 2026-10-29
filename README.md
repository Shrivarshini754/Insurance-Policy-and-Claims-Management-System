# InsureTrack — Insurance Policy Claims Management System

## About the Project

InsureTrack is a Java-based Insurance Policy Claims Management System developed as part of a Database Management Systems (DBMS) project. It provides a desktop application for managing customers, insurance policies, claims, payments, and insured assets.

This repository contains two versions of the project, developed using different database technologies.

## Projects

### Project 1 — DA2

- **Database:** Oracle Database
- **Technologies:** Java, Java Swing, SQL, PL/SQL, JDBC
- **Focus:** Relational database design, normalization, SQL queries, and database programming.

### Project 2 — DA3

- **Database:** Elasticsearch Cloud
- **Technologies:** Java, Java Swing, Elasticsearch, Elasticsearch Java Client/API, Maven
- **Focus:** Document-oriented data storage, Elasticsearch integration, CRUD operations, and cascade handling for related records.

## Key Features

- Customer and insurance policy management
- Insurance claim registration and management
- Payment and insured asset management
- Create, read, update, and delete (CRUD) operations
- Database connectivity and data retrieval
- Elasticsearch Query Console supporting read-only queries
- Handling of dependent records during supported cascade deletion operations

## Repository Structure

```text
InsureTrack/
├── Project 1 - DA2/
│   └── Oracle-based implementation
├── Project 2 - DA3/
│   ├── src/
│   │   ├── dao/
│   │   ├── db/
│   │   ├── frontend/
│   │   ├── model/
│   │   └── service/
│   ├── lib/
│   ├── pom.xml
│   └── README.md
└── README.md
```

## Technologies Used

| Component | Technology |
|---|---|
| Programming Language | Java |
| Desktop Interface | Java Swing |
| DA2 Database | Oracle Database |
| DA2 Database Programming | SQL and PL/SQL |
| DA3 Database | Elasticsearch Cloud |
| Build Tool | Apache Maven |
| Version Control | Git and GitHub |

## Running Project 2 — DA3

### Prerequisites

- Java Development Kit
- Apache Maven
- An Elasticsearch Cloud deployment
- An Elasticsearch API key with appropriate permissions

### Configure Elasticsearch

Set the following environment variables in PowerShell:

```powershell
$env:ELASTICSEARCH_URL = "YOUR_ELASTICSEARCH_ENDPOINT"
$env:ELASTICSEARCH_API_KEY = "YOUR_ELASTICSEARCH_API_KEY"
```

Replace the placeholders with your actual connection details. Do not commit API keys or other secrets to the repository.

### Build and Run

Navigate to the DA3 directory:

```powershell
cd "Project 2 - DA3"
```

Compile the project:

```powershell
mvn clean compile
```

Launch the application using the Elasticsearch backend:

```powershell
mvn exec:java "-Dexec.mainClass=frontend.MainFrame" "-Ddb.backend=elasticsearch"
```

## Purpose

This project demonstrates relational database design, document-oriented data management, database connectivity, CRUD operations, and the integration of Java desktop applications with different database technologies.

---

**Project:** Insurance Policy Claims Management System  
**Programming Language:** Java  
**Interface:** Java Swing  
**Databases:** Oracle Database and Elasticsearch Cloud  
**Tools:** SQL, PL/SQL, Maven, Git, and GitHub
