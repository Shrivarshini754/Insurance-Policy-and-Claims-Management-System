# InsureTrack — Insurance Policy Claims System (DA3)

## 1. Project Overview

InsureTrack is a Java-based Insurance Policy Claims Management System developed as part of DA3. The project extends the insurance management application by using **Neo4j, a graph database**, to store and manage insurance-related data and relationships.

The application uses Java Swing for its desktop interface and the Neo4j Java Driver to communicate with the database. It supports managing customers, policies, claims, payments, and insured assets through a structured application architecture.

## 2. Objectives

The main objectives of this project are to:

- Extend the existing Insurance Policy Claims System using a graph database.
- Understand graph database concepts, including nodes and relationships.
- Implement database operations using Cypher queries.
- Connect a Java application to Neo4j.
- Provide CRUD operations through a graphical interface.
- Organize the application using DAO and service layers.
- Maintain consistency between the frontend and the database.

## 3. Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Java |
| Frontend | Java Swing |
| Database | Neo4j |
| Query Language | Cypher |
| Connectivity | Neo4j Java Driver |
| Build Tool | Maven |
| Architecture | DAO and service layers |

## 4. Functional Modules

The application contains the following modules:

- **Customer Management:** View, add, search, update, and delete customer records.
- **Policy Management:** Manage insurance policies and their details.
- **Claim Management:** Register and maintain insurance claim information.
- **Payment Management:** Record and manage payments associated with claims.
- **Asset Management:** Maintain information about insured assets.
- **Dashboard:** Present a summarized view of insurance-related information.
- **Cypher Query Console:** Execute supported Cypher queries and inspect their results.

## 5. Graph Database Concepts

Unlike a traditional relational database, Neo4j represents data using a graph structure.

- **Nodes:** Represent entities such as customers, policies, claims, payments, and assets.
- **Relationships:** Represent connections between related entities.
- **Properties:** Store details associated with nodes and relationships.
- **Cypher:** The query language used to create, retrieve, update, and delete graph data.

This approach allows related insurance information to be represented through connected entities.

## 6. Application Architecture

The application follows a layered architecture to separate the user interface from database operations.

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
Neo4jConnection
        |
        v
Neo4j Database
```

- **Frontend Layer:** Provides forms, tables, navigation, and user interactions.
- **Service Layer:** Coordinates application operations between the frontend and DAOs.
- **DAO Layer:** Implements database operations for individual modules.
- **Connection Layer:** Establishes communication with Neo4j using the Java Driver.
- **Database Layer:** Stores insurance entities and their relationships as graph data.

## 7. Project Structure

```text
Project 2 - DA3/
├── src/
│   ├── dao/
│   ├── db/
│   ├── frontend/
│   ├── model/
│   └── service/
├── lib/
├── pom.xml
└── README.md
```

- `dao/` — Database access classes.
- `db/` — Database connection configuration.
- `frontend/` — Java Swing interface and application panels.
- `model/` — Classes used to represent application data.
- `service/` — Business and application service logic.
- `lib/` — Locally included libraries, if required.
- `pom.xml` — Maven project configuration and dependency management.

## 8. CRUD Operations

The application demonstrates the four basic database operations:

- **Create:** Add new customers, policies, claims, payments, and assets.
- **Read:** Retrieve and display records from the database.
- **Update:** Modify existing records.
- **Delete:** Remove records according to the application's deletion rules.

These operations are initiated through the frontend and processed by the backend before the results are reflected in the application.

## 9. Setup and Execution

### Prerequisites

Install the following software:

- Java Development Kit (JDK)
- Apache Maven
- Neo4j Desktop or another compatible Neo4j instance

### Setup Steps

1. Clone or download this repository.
2. Open the `Project 2 - DA3` directory.
3. Start the Neo4j database instance.
4. Configure the Neo4j connection details and credentials locally.
5. Build the project using Maven:

   ```bash
   mvn clean compile
   ```

6. Run the main application class, `frontend.MainFrame`, using the project's configured dependencies.

**Note:** The Neo4j database name, connection URI, authentication details, and dependency configuration must match the local setup. Do not commit passwords or other secrets to GitHub.

## 10. Testing

The application can be tested by verifying:

- Database connectivity.
- Record insertion and retrieval.
- Updating and deleting records.
- Correct display of records in the frontend.
- Relationships between related insurance entities.
- Execution of supported Cypher queries.
- Handling of invalid inputs and database errors.

## 11. Conclusion

InsureTrack DA3 demonstrates how a Java desktop application can integrate with a graph database to manage insurance information. The project provides practical experience with Neo4j, Cypher, database connectivity, layered architecture, and CRUD operations.

By extending the insurance management system with graph database technology, the project explores an alternative approach to storing and querying connected data.

---

**Technology focus:** Java · Java Swing · Neo4j · Cypher · Maven
