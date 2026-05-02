# Lab 11: Deploying WordPress with Docker Swarm

## Introduction
This experiment demonstrates how to deploy a WordPress website using Docker Swarm for container orchestration. You will set up a multi-container application with WordPress and MySQL, leveraging Docker Swarm's clustering and scaling features.

---

## Objectives
- Initialize a Docker Swarm cluster
- Deploy WordPress and MySQL as services
- Use persistent storage with Docker volumes
- Access the WordPress site from your browser

---

## Prerequisites
- Docker and Docker Compose installed
- Basic knowledge of Docker concepts
- Access to a terminal with administrative privileges

---

## Steps

### 1. Initialize Docker Swarm
If your system has multiple network interfaces, specify the IP address to advertise:
```sh
docker swarm init --advertise-addr <YOUR_IP_ADDRESS>
```
Replace `<YOUR_IP_ADDRESS>` with your main network IP (e.g., 172.30.10.156).

### 2. Deploy the Stack
Use the provided `docker-compose.yml` to deploy the stack as a Swarm stack:
```sh
docker stack deploy -c docker-compose.yml wordpress_stack
```

### 3. Check Service Status
List the running services:
```sh
docker service ls
```

### 4. Access WordPress
- Open your browser and go to `http://<YOUR_IP_ADDRESS>:8080`
- Complete the WordPress setup wizard

### 5. Scaling Services (Optional)
To scale the WordPress service:
```sh
docker service scale wordpress_stack_wordpress=2
```

---

## docker-compose.yml Overview
- **db**: Runs MySQL with persistent storage
- **wordpress**: Runs the WordPress application, depends on the database
- **Volumes**: `db_data` and `wp_data` ensure data persists across restarts

---

## Troubleshooting
- If you see an error about multiple IP addresses, use `--advertise-addr` as shown above.
- Ensure ports 8080 (WordPress) and 3306 (MySQL) are not blocked or in use.
- Use `docker service ps <SERVICE_NAME>` to check for errors in service startup.

---

## Conclusion
You have successfully deployed a WordPress site using Docker Swarm. This setup can be extended to include more services, enable scaling, and provide high availability.

---

Feel free to experiment with scaling and updating services to explore more Docker Swarm features!