# InsureTrack -- Insurance Policy Claims System

## 1. Project Overview

**InsureTrack** is a Java-based Insurance Policy Claims Management
System developed as a DBMS project. The system combines an Oracle
relational database with a Java Swing desktop application to manage
customers, policies, claims, payments, and insured assets.

The project demonstrates the complete database development process, from
relational design and normalization to SQL/PL/SQL implementation, JDBC
connectivity, backend processing, and frontend integration.

------------------------------------------------------------------------

## 2. Objectives

The main objectives of the project are:

-   Design and implement a relational database for an insurance
    management system.
-   Convert the conceptual/EER design into normalized relational tables.
-   Maintain data integrity using primary keys, foreign keys, and
    constraints.
-   Implement SQL queries for data insertion, retrieval, updating, and
    deletion.
-   Demonstrate PL/SQL procedures, functions, triggers, and cursors.
-   Connect the Java application to Oracle Database using JDBC.
-   Develop a clean and user-friendly Java Swing interface.
-   Provide CRUD operations for the major insurance entities.
-   Provide dashboard statistics and an interactive SQL query interface.

------------------------------------------------------------------------

## 3. Technology Stack

  Component              Technology
  ---------------------- --------------------------------------------------
  Programming Language   Java
  Frontend               Java Swing
  Database               Oracle Database
  Database Programming   SQL and PL/SQL
  Connectivity           JDBC
  JDBC Driver            Oracle `ojdbc17.jar`
  JDK                    Java 17
  IDE                    Visual Studio Code
  Architecture           Layered architecture with DAO and service layers

------------------------------------------------------------------------

## 4. Development Methodology

The project was developed in the following stages:

### Step 1 -- Requirement Analysis

The insurance domain was studied and the required entities, attributes,
relationships, and operations were identified.

The main functional areas were:

-   Customer management
-   Agent management
-   Policy management
-   Asset management
-   Policy purchase and coverage
-   Audit management
-   Claim management
-   Service provider management
-   Payment management

### Step 2 -- EER and Relational Design

The identified entities and relationships were represented using an
EER-based design and then mapped to relational tables.

Primary keys were selected to uniquely identify records, while foreign
keys were used to represent relationships between related entities.

### Step 3 -- Normalization

The relational design was organized according to normalization
principles, including 1NF, 2NF, and 3NF, to reduce redundancy and
improve consistency.

The final design separates related attributes into appropriate relations
such as the different customer, policy, claim, asset, payment, and
relationship tables.

### Step 4 -- Database Implementation

The relational schema was implemented in Oracle Database using SQL.

The `insurance_setup.sql` script creates the tables, constraints, sample
data, and PL/SQL components required by the system.

### Step 5 -- SQL and PL/SQL Development

SQL was used for database operations and retrieval.

PL/SQL was used for database-side procedural operations, including:

-   `add_customer` procedure
-   `register_claim` procedure
-   `make_payment` procedure
-   `get_claim_amount` function
-   `check_policy_expiry` trigger
-   `display_claims` cursor-based procedure

### Step 6 -- Backend Development

The backend follows a DAO-based layered structure.

Each DAO is responsible for database operations for a particular module,
while `BackendService` provides a common service-level interface for the
frontend.

### Step 7 -- JDBC Integration

`DBConnection.java` centralizes the Oracle JDBC connection. DAO classes
use this connection to execute SQL and PL/SQL operations.

### Step 8 -- Frontend Development

A Java Swing desktop application was developed with a main frame and
separate panels for each major module.

The interface provides navigation, forms, tables, search controls, CRUD
actions, dashboard statistics, and SQL query execution.

### Step 9 -- Integration and Testing

The frontend was connected to the backend service. Database operations
were tested through the application, including data retrieval,
insertion, updating, deletion, search, and validation.

------------------------------------------------------------------------

## 5. Database Design

The final database contains **24 relational tables** covering the major
insurance entities and relationships.

### Main table groups

-   `Customer1`, `Customers2`
-   `Agent1`, `Agent2`
-   `Policy1`, `Policy2`, `Policy3`
-   `Asset1`, `Asset2`
-   `Purchase1`, `Purchase2`
-   `Covers1`, `Covers2`
-   `Audit1`, `Audit2`
-   `Claim1`, `Claim2A`, `Claim2B`
-   `ServiceProvider1`, `ServiceProvider2`
-   `Payment1`, `Payment2`
-   `Involves1`, `Involves2`

Primary and foreign key constraints maintain relationships and
referential integrity between these tables.

------------------------------------------------------------------------

## 6. SQL Implementation

SQL is used throughout the project for:

-   Table creation
-   Data insertion
-   Data retrieval
-   Searching
-   Updating records
-   Deleting records
-   Aggregate calculations
-   Joining normalized relations

The DAO layer uses SQL statements to reconstruct meaningful
application-level records from the normalized database structure.

------------------------------------------------------------------------

## 7. PL/SQL Components

### `add_customer`

Adds a new customer to the database using a stored procedure.

### `register_claim`

Registers a new insurance claim by inserting the required claim
information.

### `make_payment`

Records a payment associated with a claim.

### `get_claim_amount`

Returns the claimed amount for a specified claim.

### `check_policy_expiry`

A database trigger that validates the incident date when a new claim is
inserted. A claim is rejected when its incident date is later than the
associated policy expiry date.

### `display_claims`

Uses an explicit cursor to retrieve and process claim information from
the related claim tables.

------------------------------------------------------------------------

## 8. Backend Architecture

The backend is organized into separate layers:

``` text
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

### DAO Classes

The project contains DAO classes for:

-   Customers
-   Policies
-   Claims
-   Payments
-   Assets
-   SQL query execution

### BackendService

`BackendService.java` acts as the service interface between the frontend
and DAO layer.

It provides operations for:

-   Customer CRUD
-   Policy CRUD
-   Claim operations
-   Payment operations
-   Asset CRUD
-   Dashboard statistics
-   Recent claims
-   SQL query execution

------------------------------------------------------------------------

## 9. Frontend Architecture

The frontend is implemented using Java Swing.

### MainFrame

`MainFrame.java` acts as the main application window and provides:

-   Application header
-   Navigation sidebar
-   Page switching using `CardLayout`
-   Dashboard access
-   Module navigation
-   Application status and user information

### Functional Panels

The application contains separate panels for:

-   `DashboardPanel`
-   `CustomerPanel`
-   `PolicyPanel`
-   `ClaimPanel`
-   `PaymentPanel`
-   `AssetPanel`
-   `SQLQueryPanel`

The interface uses tables, forms, buttons, search controls, and
validation messages to provide a consistent desktop application
experience.

------------------------------------------------------------------------

## 10. Functional Modules

### Customer Management

Users can:

-   View customers
-   Search customers
-   Add customers
-   Update customers
-   Delete customers
-   Refresh records

### Policy Management

Users can:

-   View policies
-   Search policies
-   Add policies
-   Update policies
-   Delete policies
-   Refresh records

### Claim Management

Users can:

-   View claims
-   Register claims
-   Update claims
-   Delete claims
-   Retrieve claim amounts
-   Refresh records

### Payment Management

Users can:

-   View payments
-   Add payments
-   Update payments
-   Delete payments
-   Refresh records

### Asset Management

Users can:

-   View assets
-   Add assets
-   Update assets
-   Delete assets
-   Refresh records

------------------------------------------------------------------------

## 11. Dashboard

The dashboard provides a summarized view of the insurance system.

It can display:

-   Total customers
-   Total policies
-   Active policies
-   Total claims
-   Claims for the current year
-   Total payments
-   Total assets
-   Premium and claim/payment totals
-   Recent claims

These values are obtained through backend service methods rather than
being permanently stored as UI values.

------------------------------------------------------------------------

## 12. SQL Query Module

The application includes an interactive SQL query panel.

The workflow is:

``` text
User enters SQL
       |
       v
SQLQueryPanel
       |
       v
BackendService
       |
       v
QueryDAO
       |
       v
Oracle Database
       |
       v
QueryResult
       |
       v
Display result in JTable
```

This module demonstrates how database query results can be retrieved
dynamically and displayed through the Java application.

------------------------------------------------------------------------

## 13. CRUD Workflow

A typical CRUD operation follows this flow:

``` text
User Action
    |
    v
Swing Frontend
    |
    v
BackendService
    |
    v
DAO
    |
    v
JDBC
    |
    v
Oracle Database
    |
    v
Result
    |
    v
Frontend Table / Message
```

For example, when a customer is added:

``` text
Customer Form
     ↓
Input Validation
     ↓
BackendService.addCustomer()
     ↓
CustomerDAO
     ↓
Oracle / PL/SQL
     ↓
Success or Error
     ↓
Refresh Customer Table
```

------------------------------------------------------------------------

## 14. Validation and Data Integrity

Data integrity is maintained at multiple levels.

### Application Level

The frontend validates user input before submitting database operations.

### Backend Level

DAO and service methods handle database-related exceptions and return
operation results to the frontend.

### Database Level

Oracle enforces:

-   Primary key constraints
-   Foreign key constraints
-   Data types
-   Referential integrity
-   PL/SQL exception handling
-   Policy expiry validation through a trigger

------------------------------------------------------------------------

## 15. Project Structure

``` text
InsuranceProject/
│
├── insurance_setup.sql
│
├── lib/
│   └── ojdbc17.jar
│
├── src/
│   ├── dao/
│   │   ├── AssetDAO.java
│   │   ├── ClaimDAO.java
│   │   ├── CustomerDAO.java
│   │   ├── PaymentDAO.java
│   │   ├── PolicyDAO.java
│   │   └── QueryDAO.java
│   │
│   ├── db/
│   │   └── DBConnection.java
│   │
│   ├── frontend/
│   │   ├── AssetPanel.java
│   │   ├── ClaimPanel.java
│   │   ├── CustomerPanel.java
│   │   ├── DashboardPanel.java
│   │   ├── MainFrame.java
│   │   ├── PaymentPanel.java
│   │   ├── PolicyPanel.java
│   │   └── SQLQueryPanel.java
│   │
│   ├── model/
│   │   └── QueryResult.java
│   │
│   └── service/
│       ├── BackendService.java
│       └── TestBackend.java
│
└── out/
```

------------------------------------------------------------------------

## 16. Installation and Setup

### Prerequisites

Install:

-   Java JDK 17 or compatible JDK
-   Oracle Database
-   SQL\*Plus or another Oracle SQL client
-   Visual Studio Code or another Java IDE

The Oracle JDBC driver `ojdbc17.jar` must be available in the project's
`lib` folder.

### Database Setup

1.  Start Oracle Database.
2.  Connect to the `INSURANCE` schema.
3.  Execute:

``` text
insurance_setup.sql
```

4.  Verify that the tables and PL/SQL objects are created successfully.
5.  Verify that the Java database connection configuration matches the
    local Oracle environment.

> **Note:** Database credentials should be configured locally and should
> not be committed to a public GitHub repository.

------------------------------------------------------------------------

## 17. Running the Application

From the project root, compile the Java source files with the JDBC
driver available on the classpath.

Example:

``` powershell
javac -encoding UTF-8 -cp "lib\ojdbc17.jar" -d out `
src\dao\*.java `
src\db\*.java `
src\model\*.java `
src\service\*.java `
src\frontend\*.java
```

Then run:

``` powershell
java -cp "out;lib\ojdbc17.jar" frontend.MainFrame
```

The application opens the InsureTrack desktop interface.

------------------------------------------------------------------------

## 18. Testing

The project was tested at multiple levels:

### Database Testing

-   Table creation
-   Record insertion
-   Primary/foreign key constraints
-   PL/SQL procedures
-   Function execution
-   Trigger validation
-   Cursor execution

### Backend Testing

-   DAO operations
-   Service methods
-   Database connection
-   CRUD operations
-   Dashboard queries
-   SQL query execution

### Frontend Testing

-   Application startup
-   Navigation
-   Form validation
-   Table loading
-   Search
-   Add
-   Update
-   Delete
-   Refresh
-   Error messages
-   Database-backed data display

### Integration Testing

The final application was tested through the complete flow:

``` text
Frontend → BackendService → DAO → JDBC → Oracle
```

and the returned database results were displayed back in the Swing
interface.

------------------------------------------------------------------------

## 19. Team Contribution

The project was developed as a team with responsibilities divided across
different layers.

### Frontend

-   Java Swing interface
-   Main application frame
-   Dashboard
-   Customer, Policy, Claim, Payment, and Asset panels
-   Navigation and UI design

### Backend / Database

-   Oracle database implementation
-   SQL and PL/SQL
-   DAO layer
-   Backend service layer
-   Database connectivity

### Integration

-   Connecting frontend actions with backend service methods
-   Loading database records into Swing tables
-   Connecting CRUD operations
-   Testing the complete application workflow

------------------------------------------------------------------------

## 20. Conclusion

InsureTrack demonstrates the development of a complete database-driven
application using **Oracle Database and Java**.

The project covers the major stages of DBMS application development:

``` text
Requirement Analysis
        ↓
EER / Relational Design
        ↓
Normalization
        ↓
Oracle Database Implementation
        ↓
SQL & PL/SQL
        ↓
DAO / Backend Development
        ↓
JDBC Connectivity
        ↓
Java Swing Frontend
        ↓
Integration
        ↓
Testing
```

The final system provides a structured way to manage insurance
customers, policies, claims, payments, and assets while demonstrating
important DBMS concepts such as normalization, relational integrity,
SQL, PL/SQL procedures, functions, triggers, cursors, JDBC, and CRUD
operations.
