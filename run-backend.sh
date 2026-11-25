#!/usr/bin/env bash
# Script pembantu untuk menjalankan backend Logbook dengan database logbook_db di port 5432
cd "$(dirname "$0")"
echo "Menjalankan Spring Boot backend (logbook-be) pada database logbook_db..."
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/logbook_db ./mvnw spring-boot:run
