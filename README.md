This project is a simple Netty-based HTTP server with Redis integration.
Redis is used to store information about application errors and exceptions.

The project is fully containerized and can be started with a single command.

## Requirements
The only required tools are:

- Docker
- Docker Compose

No Java or Maven installation is required on the host machine.

## How to Run
1. Clone the repository or extract the provided archive:

tar -xzf netty-dashboard-demo.tar.gz
cd netty-dashboard-demo

2. Build and start the application:

docker-compose up --build

The Netty server will start on port 8080.

## API Usage
Valid request

curl http://localhost:8080/api/dashboard

Expected result:
HTTP 200
Successful response from the server

Invalid request (error case)

curl http://localhost:8080/api/invalid-endpoint
Expected result:
HTTP 404

Error information is stored in Redis

## Redis Verification
To inspect stored errors in Redis:

docker exec -it redis redis-cli

KEYS *

## Stopping the application
To stop all containers:

docker-compose down

All dependencies are resolved inside Docker containers.
Java version used inside containers: Java 21 (Eclipse Temurin).
