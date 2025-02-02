# AutoFine Project

## Overview

AutoFine is a distributed microservices system designed to process data from speed cameras, issue fines, manage driving license suspensions, and notify users about their penalties via email. The project is built using Java with Spring Boot and employs Apache Kafka for asynchronous communication between services. It adheres to Clean Code principles and includes static code analysis tools.

## Key Features

*   **Microservice Architecture:** The system is divided into independent microservices for scalability and maintainability.
*   **Asynchronous Communication:** Services communicate via Apache Kafka for decoupled and reliable message handling.
*   **Data Processing:**  Handles speed camera data, validates it, and converts units as needed.
*   **Fine Management:**  Issues fines based on processed speed camera data, linking them to users and vehicles.
*   **Driving License Management:**  Manages license suspension and reinstatement, based on points and fine payments.
*   **Notification System:** Notifies users about issued fines and license status updates via email.
*   **Technology Stack:** Java 21, Spring Boot, PostgreSQL.

## Services

The system consists of the following microservices:

1.  **Fotoradar Data Service:**
    *   Responsible for receiving data from speed cameras, performing initial validation, and preprocessing.
    *   Consumes raw speed camera data and publishes processed data.

2.  **Mandate Service:**
    *   Responsible for issuing fines based on processed speed camera data.
    *   Creates records for issued fines in the database.

3.  **Driving License Service:**
    *   Manages driving license statuses, including suspensions and reinstatements.
    *   Updates license statuses based on fines and other external events.

4.  **Notification Service:**
    *   Sends email notifications to users about issued fines and license status updates.

## Data Flow

The system follows this general data flow:
   
   1. Speed camera data is received by the Fotoradar Data Service, which validates and processes it.
   2. Processed data is sent to the Mandate Service which issues a fine and stores it.
   3. The Mandate Service notifies the Driving License Service that a fine has been issued.
   4. The Notification Service is notified of issued fines and license updates and sends emails to users.

## Technologies Used

*   Java 21
*   Spring Boot
*   PostgreSQL
*   Apache Kafka

## Getting Started

To run the project, please follow these steps:

1.  Clone this repository.
2.  Build the project using Maven: `mvn clean install`
3.  Refer to the full project documentation for detailed instructions on running each microservice.
