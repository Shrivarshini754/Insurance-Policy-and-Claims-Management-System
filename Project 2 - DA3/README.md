\# Project 2 - DA3: Insurance Policy Claims Management System



\## Overview



This project implements an Insurance Policy Claims Management System using Java Swing and Elasticsearch Cloud. It provides a graphical interface for managing customers, insurance policies, claims, payments, and insured assets.



\## Technology Stack



\- Java

\- Java Swing

\- Elasticsearch Cloud

\- Apache Maven



\## Features



\- Customer CRUD operations

\- Insurance policy management

\- Claims registration and management

\- Payment management

\- Insured asset management

\- Elasticsearch connectivity

\- Cascade deletion of dependent records where supported

\- Read-only Elasticsearch Query Console



\## Elasticsearch Indexes



\- `insurance\_customers`

\- `insurance\_policies`

\- `insurance\_claims`

\- `insurance\_payments`

\- `insurance\_assets`

\- `insurance\_service\_providers`



\## Prerequisites



\- Java Development Kit

\- Apache Maven

\- Elasticsearch Cloud deployment

\- Elasticsearch API key with appropriate permissions



\## Configuration



Set the Elasticsearch connection details in PowerShell:



```powershell

$env:ELASTICSEARCH\_URL = "YOUR\_ELASTICSEARCH\_ENDPOINT"

$env:ELASTICSEARCH\_API\_KEY = "YOUR\_ELASTICSEARCH\_API\_KEY"

```



Replace the placeholders with your own values. Do not commit credentials to the repository.



\## Build and Run



From the DA3 project directory:



```powershell

mvn clean compile

```



Run the application with the Elasticsearch backend:



```powershell

mvn exec:java "-Dexec.mainClass=frontend.MainFrame" "-Ddb.backend=elasticsearch"

```



\## Integration Testing



Run the Elasticsearch cascade CRUD integration test:



```powershell

mvn exec:java "-Dexec.mainClass=service.ElasticsearchCascadeCRUDIntegrationTest"

```



The test requires a configured Elasticsearch connection.



\## Project Structure



\- `src/` — Java source code

\- `lib/` — Project libraries

\- `pom.xml` — Maven build configuration

\- `insurance\_setup.sql` — SQL setup script retained from the project



\## Security



Never commit Elasticsearch API keys, passwords, or other secrets. Use environment variables and restrict API key permissions.



\## Academic Project



This project was developed as part of a Database Management Systems assignment.



