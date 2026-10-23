# InsureTrack — Insurance Policy Claims System (DA3)

## Overview
InsureTrack DA3 extends the Insurance Policy Claims System using **Neo4j**, a graph database. The application uses Java Swing for the frontend and Java for database operations.

## Technologies Used
- **Language:** Java
- **Frontend:** Java Swing
- **Database:** Neo4j
- **Query Language:** Cypher
- **Database Connectivity:** Neo4j Java Driver
- **Build Tool:** Maven

## Features
- Customer, policy, claim, payment, and asset management
- CRUD operations (Create, Read, Update, Delete)
- Graph-based storage of insurance data and relationships
- Dashboard and data visualization
- Cypher query execution

## Project Structure
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

## Setup
1. Install Java JDK, Maven, and Neo4j.
2. Start the Neo4j database.
3. Configure the Neo4j connection and credentials locally.
4. Build the project using Maven.
5. Run `frontend.MainFrame` to launch the application.

## Purpose
This project demonstrates graph database concepts, Cypher queries, Java database connectivity, and CRUD operations in an insurance management application.

**Note:** Do not commit database passwords or other secrets to GitHub.
