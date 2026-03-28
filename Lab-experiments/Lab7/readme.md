# Lab Experiment 7: CI/CD Pipeline Using Jenkins, GitHub, and Docker

## 1. Aim
To design and implement a complete **Continuous Integration/Continuous Deployment (CI/CD)** pipeline using Jenkins, integrating source code from GitHub, and automating Docker image building and pushing to Docker Hub.

---

## 2. Objectives

- ✅ Understand CI/CD workflow using Jenkins (GUI-based tool)
- ✅ Create a structured GitHub repository with application code and Jenkinsfile
- ✅ Build Docker images from source code automatically
- ✅ Securely store Docker Hub credentials in Jenkins
- ✅ Automate build & push process using webhook triggers
- ✅ Use the same host (Docker) as Jenkins agent for pipeline execution

---

## 3. Prerequisites

Before starting this lab, ensure you have:

### Hardware Requirements
- **Minimum 4GB RAM** (8GB recommended)
- **20GB free disk space**
- **Multi-core processor** (2+ cores)

### Software Requirements
- Linux/Ubuntu (20.04 LTS or later)
- Docker & Docker Compose installed
- Git installed
- Java 11+ (for Jenkins)
- GitHub account (with repository access)
- Docker Hub account

### Verification Commands
```bash
# Check Docker installation
docker --version

# Check Docker Compose
docker-compose --version

# Check Git
git --version

# Check Java
java -version
```

---

## 4. Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                     GitHub Repository                         │
│              (Source Code + Jenkinsfile)                      │
└─────────────────────────────┬──────────────────────────────────┘
                              │
                          (Webhook)
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                      Jenkins Server                           │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Pipeline Job                                           │ │
│  │  - Trigger on GitHub push                              │ │
│  │  - Checkout code from GitHub                           │ │
│  │  - Build Stage (Compile/Test)                          │ │
│  │  - Docker Build Stage                                  │ │
│  │  - Push to Docker Hub                                  │ │
│  │  - Deploy (Optional)                                   │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────┬───────────────────────────────────┬────────┘
                   │                                   │
             (SSH/Docker Socket)                  (Credentials)
                   │                                   │
                   ▼                                   ▼
┌──────────────────────────────┐          ┌──────────────────────┐
│   Docker Agent (Same Host)   │          │   Docker Hub Registry │
│  - Executes pipeline stages  │          │  (Image Storage)     │
│  - Builds Docker images      │          └──────────────────────┘
│  - Runs tests in containers  │
└──────────────────────────────┘
```

---

## 5. Step-by-Step Implementation

### Step 1: Install and Configure Jenkins

#### 1.1 Using Docker (Recommended)

```bash
# Create Jenkins home directory
mkdir -p ~/jenkins_home
chmod 777 ~/jenkins_home

# Run Jenkins container
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v ~/jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /usr/bin/docker:/usr/bin/docker \
  --user root \
  jenkins/jenkins:lts

# Wait for Jenkins to start (30-60 seconds)
sleep 30

# Get Jenkins initial admin password
docker logs jenkins 2>&1 | grep "Initial AdminPassword"
```

#### 1.2 Manual Installation (Alternative)

```bash
# Update package manager
sudo apt update && sudo apt upgrade -y

# Install Java
sudo apt install openjdk-11-jdk -y

# Add Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

# Install Jenkins
sudo apt-get update
sudo apt-get install jenkins -y

# Start Jenkins service
sudo systemctl start jenkins
sudo systemctl enable jenkins

# Get initial admin password
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

#### 1.3 Initial Setup
1. Open browser: `http://localhost:8080`
2. Enter the initial admin password from logs
3. Click "Install suggested plugins"
4. Create admin user
5. Configure Jenkins URL (use `http://localhost:8080`)

---

### Step 2: Configure Jenkins for Docker

#### 2.1 Add Docker Socket Access

```bash
# If Jenkins is running in container
docker exec -u root jenkins bash -c "apt-get update && apt-get install -y docker.io"

# Add Jenkins user to docker group (for direct installation)
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

#### 2.2 Install Required Jenkins Plugins

1. **Manage Jenkins** → **Manage Plugins**
2. Search and install:
   - **GitHub Plugin**
   - **Docker Pipeline**
   - **Docker Commons Plugin**
   - **Credentials Binding Plugin**
   - **Pipeline Plugin**

3. Restart Jenkins

---

### Step 3: Configure Credentials in Jenkins

#### 3.1 Add Docker Hub Credentials

1. Navigate to **Manage Jenkins** → **Manage Credentials**
2. Click **System** → **Global credentials (unrestricted)**
3. Click **Add Credentials**
4. Configure:
   - **Kind**: Username with password
   - **Username**: Your Docker Hub username
   - **Password**: Your Docker Hub access token (NOT password)
   - **ID**: `docker-hub-creds`
   - **Description**: Docker Hub Credentials
5. Click **Create**

#### 3.2 Add GitHub Credentials (Optional - for Private Repos)

1. Generate GitHub Personal Access Token:
   - GitHub Settings → Developer settings → Personal access tokens
   - Create token with `repo` and `admin:repo_hook` scopes
2. In Jenkins **Manage Credentials**:
   - **Kind**: Username with password
   - **Username**: Your GitHub username
   - **Password**: The PAT token
   - **ID**: `github-creds`
3. Click **Create**

---

### Step 4: Create GitHub Repository Structure

#### 4.1 Sample GitHub Repository Layout

```
my-app/
├── Dockerfile
├── Jenkinsfile
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── example/
│   │               └── App.java
│   └── test/
│       └── java/
│           └── TestApp.java
├── pom.xml (for Maven) or package.json (for Node.js)
├── .gitignore
└── README.md
```

#### 4.2 Sample Application Files

**Dockerfile** (Java/Maven example):
```dockerfile
# Build stage
FROM maven:3.8-openjdk-11 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

**Dockerfile** (Node.js example):
```dockerfile
# Build stage
FROM node:16 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

# Runtime stage
FROM node:16-alpine
WORKDIR /app
COPY --from=builder /app/node_modules ./node_modules
COPY . .
EXPOSE 3000
CMD ["node", "server.js"]
```

#### 4.3 Jenkinsfile (Pipeline Script)

**Jenkinsfile** - Declarative Pipeline:
```groovy
pipeline {
    agent {
        docker {
            image 'docker:latest'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        string(
            name: 'DOCKER_REGISTRY',
            defaultValue: 'docker.io',
            description: 'Docker Registry URL'
        )
        string(
            name: 'IMAGE_NAME',
            defaultValue: 'myapp',
            description: 'Docker Image Name'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo '===== Checking out source code ====='
                }
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    echo '===== Building Application ====='
                    // Uncomment based on your tech stack
                    
                    // For Maven
                    // sh 'mvn clean package'
                    
                    // For Node.js
                    // sh 'npm install && npm test'
                    
                    // For Python
                    // sh 'pip install -r requirements.txt && python -m pytest'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo '===== Building Docker Image ====='
                    sh '''
                        IMAGE_TAG="${DOCKER_REGISTRY}/${DOCKER_USERNAME}/${IMAGE_NAME}:${BUILD_NUMBER}"
                        IMAGE_LATEST="${DOCKER_REGISTRY}/${DOCKER_USERNAME}/${IMAGE_NAME}:latest"
                        
                        docker build -t ${IMAGE_TAG} -t ${IMAGE_LATEST} .
                        
                        echo "Image Tag: ${IMAGE_TAG}"
                        echo "Image Latest: ${IMAGE_LATEST}"
                    '''
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    echo '===== Pushing to Docker Hub ====='
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-creds',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh '''
                            echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
                            
                            IMAGE_TAG="${DOCKER_REGISTRY}/${DOCKER_USERNAME}/${IMAGE_NAME}:${BUILD_NUMBER}"
                            IMAGE_LATEST="${DOCKER_REGISTRY}/${DOCKER_USERNAME}/${IMAGE_NAME}:latest"
                            
                            docker push ${IMAGE_TAG}
                            docker push ${IMAGE_LATEST}
                            
                            docker logout
                        '''
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                script {
                    echo '===== Cleaning up Docker images ====='
                    sh '''
                        docker image prune -f --filter "until=72h"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline executed successfully!'
        }
        failure {
            echo '❌ Pipeline failed! Check logs for details.'
        }
        always {
            cleanWs()
        }
    }
}
```

**Jenkinsfile** - Scripted Pipeline (Alternative):
```groovy
node {
    stage('Checkout') {
        checkout scm
    }
    
    stage('Build & Test') {
        // Build commands here
    }
    
    stage('Build Docker Image') {
        sh 'docker build -t myapp:${BUILD_NUMBER} .'
    }
    
    stage('Push to Registry') {
        withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
            sh '''
                echo $PASS | docker login -u $USER --password-stdin
                docker push myapp:${BUILD_NUMBER}
            '''
        }
    }
}
```

---

### Step 5: Create Jenkins Pipeline Job

#### 5.1 Create a New Pipeline Job

1. Click **New Item**
2. Enter job name: `my-app-pipeline`
3. Select **Pipeline** job type
4. Click **OK**

#### 5.2 Configure the Pipeline Job

1. **General** tab:
   - Check: "GitHub project"
   - Project URL: `https://github.com/YOUR_USERNAME/my-app`

2. **Build Triggers** tab:
   - Check: "GitHub hook trigger for GITScm polling"
   - (We'll configure the webhook in GitHub)

3. **Pipeline** tab:
   - **Definition**: "Pipeline script from SCM"
   - **SCM**: Select "Git"
   - **Repository URL**: `https://github.com/YOUR_USERNAME/my-app.git`
   - **Branch**: `*/main` or `*/master`
   - **Script Path**: `Jenkinsfile`

4. Click **Save**

---

### Step 6: Configure GitHub Webhook

#### 6.1 Add Webhook to GitHub Repository

1. Go to GitHub repository: Settings → Webhooks
2. Click **Add webhook**
3. Configure:
   - **Payload URL**: `http://YOUR_JENKINS_IP:8080/github-webhook/`
   - **Content type**: `application/json`
   - **Events**: Select "Push events" and "Pull request events"
   - **Active**: Check this box
4. Click **Add webhook**

#### 6.2 Verify Webhook Connection

- Look for green checkmark next to webhook in GitHub
- Check Jenkins logs: `docker logs jenkins` (if using Docker)
- Or: `sudo tail -f /var/log/jenkins/jenkins.log`

---

### Step 7: Configure Docker Agent

#### 7.1 Setup Jenkins Agent on Same Host

1. **Manage Jenkins** → **Manage Nodes and Clouds**
2. Click **Configure System**
3. Find **Cloud** section:
   - Click **Add a new cloud** → **Docker**
   - **Docker Host URI**: `unix:///var/run/docker.sock`
   - **Test Connection** (should show Docker version)

4. Create Docker agent template:
   - **Labels**: `docker-agent`
   - **Docker Image**: `jenkins/agent:latest`
   - **Memory Limit**: `1024 MB`

---

### Step 8: Test the Pipeline

#### 8.1 Manual Trigger

1. Go to Jenkins job: `my-app-pipeline`
2. Click **Build Now**
3. Monitor build progress in **Build History**
4. Click on build number to see **Console Output**

#### 8.2 Verify Automated Trigger

1. Make a commit to your GitHub repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/my-app.git
   cd my-app
   echo "test change" >> README.md
   git add .
   git commit -m "Test webhook trigger"
   git push origin main
   ```

2. Jenkins should automatically start a new build
3. Check Jenkins dashboard for running build

---

## 6. Configuration Files Reference

### 6.1 Docker Compose for Complete Setup

Create `docker-compose.yml` for entire stack:

```yaml
version: '3.8'

services:
  jenkins:
    image: jenkins/jenkins:lts
    container_name: jenkins
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
      - /usr/bin/docker:/usr/bin/docker
    environment:
      - JENKINS_OPTS=--httpPort=8080
    user: root
    networks:
      - ci-cd-network

  docker-proxy:
    image: alpine/socat
    container_name: docker-proxy
    command: TCP-LISTEN:2375,reuseaddr,fork UNIX-CONNECT:/var/run/docker.sock
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - ci-cd-network
    ports:
      - "2375:2375"

volumes:
  jenkins_home:

networks:
  ci-cd-network:
    driver: bridge
```

### 6.2 GitHub Actions Alternative (Optional)

If you want to use GitHub Actions instead of Jenkins:

Create `.github/workflows/docker-build.yml`:

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2

    - name: Login to Docker Hub
      uses: docker/login-action@v2
      with:
        username: ${{ secrets.DOCKER_USERNAME }}
        password: ${{ secrets.DOCKER_PASSWORD }}

    - name: Build and push
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: ${{ secrets.DOCKER_USERNAME }}/myapp:latest,${{ secrets.DOCKER_USERNAME }}/myapp:${{ github.run_number }}
```

---

## 7. Troubleshooting Guide

### Issue 1: Jenkins Cannot Access Docker Socket

**Symptoms**: Docker build stage fails with "permission denied"

**Solution**:
```bash
# If Jenkins is running in container
docker exec -u root jenkins bash -c "usermod -aG docker jenkins"

# Or from host
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### Issue 2: GitHub Webhook Not Triggering Builds

**Symptoms**: Manual builds work, but auto-trigger doesn't

**Solutions**:
- Check Jenkins IP is accessible from GitHub (use ngrok for local testing)
- Verify webhook URL in GitHub: Settings → Webhooks
- Check Jenkins logs for webhook errors
- Ensure GitHub hook plugin is installed

```bash
# Jenkins logs (Docker)
docker logs -f jenkins

# Jenkins logs (Direct)
sudo tail -f /var/log/jenkins/jenkins.log
```

### Issue 3: Docker Push Authentication Fails

**Symptoms**: "unauthorized: authentication required"

**Solutions**:
- Verify Docker Hub credentials in Jenkins
- Use Personal Access Token (not password) for Docker Hub
- Check credential ID matches in Jenkinsfile
- Regenerate token if token is expired

### Issue 4: Insufficient Disk Space

**Symptoms**: Docker build fails with "No space left on device"

**Solution**:
```bash
# Clean up old images
docker image prune -a -f

# Clean up old containers
docker container prune -f

# Check disk usage
df -h
du -sh ~/jenkins_home
```

### Issue 5: Pipeline Timeout

**Symptoms**: Build takes too long or times out

**Solution**: Adjust timeout in Jenkinsfile:
```groovy
options {
    timeout(time: 60, unit: 'MINUTES')  // Increase timeout
}
```

---

## 8. Security Best Practices

### 8.1 Credential Management
- ✅ Use Jenkins Credentials Store for all secrets
- ✅ Never hardcode credentials in Jenkinsfile
- ✅ Use Personal Access Tokens instead of passwords
- ✅ Rotate tokens regularly (every 90 days)
- ✅ Use credential binding plugins

### 8.2 Jenkins Security
- ✅ Change default Jenkins port (not 8080)
- ✅ Enable HTTPS/TLS
- ✅ Set up firewall rules (allow only your IPs)
- ✅ Keep Jenkins and plugins updated
- ✅ Implement Jenkins authentication
- ✅ Use Jenkins role-based access control (RBAC)

### 8.3 Docker Security
- ✅ Use minimal base images (Alpine)
- ✅ Don't run containers as root
- ✅ Use read-only file systems where possible
- ✅ Scan images for vulnerabilities
- ✅ Use image signing and verification

### 8.4 GitHub Security
- ✅ Use branch protection rules
- ✅ Require code reviews
- ✅ Limit webhook access to Jenkins IP only
- ✅ Use GitHub Secrets for sensitive data

---

## 9. Advanced Configurations

### 9.1 Multi-Stage Build Pipeline

```groovy
pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'docker.io'
        IMAGE_NAME = 'myapp'
    }

    stages {
        stage('Code Quality') {
            steps {
                echo 'Running SonarQube analysis...'
                // sonar analysis commands
            }
        }

        stage('Security Scanning') {
            steps {
                echo 'Scanning Docker image for vulnerabilities...'
                sh 'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image ${DOCKER_REGISTRY}/${DOCKER_USERNAME}/${IMAGE_NAME}:latest'
            }
        }

        stage('Deploy to Development') {
            when {
                branch 'develop'
            }
            steps {
                echo 'Deploying to DEV environment...'
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                echo 'Deploying to PROD environment...'
            }
        }
    }
}
```

### 9.2 Parallel Builds

```groovy
stages {
    stage('Build & Test') {
        parallel {
            stage('Unit Tests') {
                steps {
                    sh 'mvn test'
                }
            }
            stage('Integration Tests') {
                steps {
                    sh 'mvn verify'
                }
            }
        }
    }
}
```

---

## 10. Performance Optimization

### 10.1 Docker Layer Caching

```dockerfile
FROM ubuntu:20.04

# Install dependencies (less frequently changed)
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    maven \
    git

# Copy application code (frequently changed)
COPY . /app
WORKDIR /app

RUN mvn clean package
```

### 10.2 Jenkins Optimization

- Use agent labels to distribute builds
- Implement build queue management
- Cache dependencies (Maven, npm, pip)
- Use sparse checkouts for large repos

### 10.3 Docker Build Cache

```bash
# Build with BuildKit for better caching
DOCKER_BUILDKIT=1 docker build -t myapp:latest .

# Use docker-compose for production
docker-compose -f docker-compose.prod.yml up -d
```

---

## 11. Monitoring and Logging

### 11.1 Jenkins Monitoring

```bash
# Monitor build queue
curl http://localhost:8080/api/json | jq '.queue'

# Check system load
curl http://localhost:8080/metrics/prometheus

# View job history
curl http://localhost:8080/job/my-app-pipeline/api/json
```

### 11.2 Docker Logging

```bash
# View Jenkins container logs
docker logs -f jenkins

# View specific build logs
docker exec jenkins cat /var/jenkins_home/jobs/my-app-pipeline/builds/1/log

# Monitor Docker daemon
journalctl -u docker -f
```

### 11.3 Pipeline Logging

Add logging to Jenkinsfile:
```groovy
stages {
    stage('Build') {
        steps {
            script {
                def timestamp = new Date().format('yyyy-MM-dd HH:mm:ss')
                echo "[${timestamp}] Starting build process..."
            }
        }
    }
}
```

---

## 12. Common Use Cases

### 12.1 Multi-Environment Deployment

```groovy
parameters {
    choice(name: 'ENVIRONMENT', choices: ['DEV', 'STAGING', 'PROD'], description: 'Deployment environment')
}

stages {
    stage('Deploy') {
        steps {
            script {
                if (params.ENVIRONMENT == 'DEV') {
                    sh './deploy-dev.sh'
                } else if (params.ENVIRONMENT == 'STAGING') {
                    sh './deploy-staging.sh'
                } else if (params.ENVIRONMENT == 'PROD') {
                    sh './deploy-prod.sh'
                }
            }
        }
    }
}
```

### 12.2 Database Migration

```groovy
stage('Database Migration') {
    steps {
        sh '''
            docker run --rm \
              -e DB_HOST=${DB_HOST} \
              -e DB_USER=${DB_USER} \
              -e DB_PASS=${DB_PASS} \
              myapp:latest \
              /app/scripts/migrate-db.sh
        '''
    }
}
```

### 12.3 Automated Testing

```groovy
stage('Run Tests') {
    steps {
        sh '''
            docker build -t myapp:test --target test .
            docker run --rm myapp:test
        '''
    }
}
```

---

## 13. Cleanup and Maintenance

### 13.1 Regular Maintenance Tasks

```bash
# Clean old builds from Jenkins
curl -X POST http://localhost:8080/job/my-app-pipeline/doDelete

# Remove dangling images
docker image prune -f

# Remove stopped containers
docker container prune -f

# Remove unused volumes
docker volume prune -f
```

### 13.2 Backup Jenkins Configuration

```bash
# Backup Jenkins home directory
sudo tar -czf jenkins_backup_$(date +%Y%m%d).tar.gz /var/lib/jenkins

# Or for Docker
docker exec jenkins tar -czf /var/jenkins_home/backup_$(date +%Y%m%d).tar.gz /var/jenkins_home
```

---

## 14. References and Resources

### Official Documentation
- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [GitHub Webhooks](https://docs.github.com/en/developers/webhooks-and-events/webhooks)
- [Docker Documentation](https://docs.docker.com/)
- [Docker Hub](https://hub.docker.com/)

### Useful Plugins
- Pipeline: https://plugins.jenkins.io/workflow-aggregator/
- Docker: https://plugins.jenkins.io/docker-plugin/
- GitHub: https://plugins.jenkins.io/github/
- Credentials: https://plugins.jenkins.io/credentials/

### Learning Resources
- Declarative Pipeline Syntax: https://www.jenkins.io/doc/book/pipeline/syntax/
- Docker Best Practices: https://docs.docker.com/develop/dev-best-practices/
- CI/CD Best Practices: https://www.atlassian.com/continuous-delivery

---

## 15. Summary Checklist

Use this checklist to verify all components are configured:

- [ ] Jenkins installed and running
- [ ] Docker and Docker Compose installed
- [ ] Java 11+ installed
- [ ] Jenkins plugins installed (Docker, GitHub, Pipeline)
- [ ] Docker Hub credentials configured in Jenkins
- [ ] GitHub credentials configured (if private repo)
- [ ] GitHub repository created with Dockerfile and Jenkinsfile
- [ ] Jenkins pipeline job created
- [ ] GitHub webhook configured
- [ ] Docker agent configured
- [ ] First build triggered successfully
- [ ] Docker image built and pushed to Docker Hub
- [ ] Automated trigger via webhook tested
- [ ] Security best practices implemented

---

## 16. Quick Start Commands

```bash
# 1. Clone repository
git clone https://github.com/YOUR_USERNAME/my-app.git
cd my-app

# 2. Start Jenkins with Docker Compose
docker-compose up -d

# 3. Access Jenkins
open http://localhost:8080

# 4. View Jenkins logs
docker logs -f jenkins

# 5. Build Docker image manually (for testing)
docker build -t myapp:latest .

# 6. Run container locally
docker run -p 8080:8080 myapp:latest

# 7. Push to Docker Hub manually (for testing)
docker login
docker tag myapp:latest YOUR_DOCKER_USERNAME/myapp:latest
docker push YOUR_DOCKER_USERNAME/myapp:latest

# 8. Cleanup
docker-compose down
docker image prune -a -f
docker volume prune -f
```

---
## Screenshots
![screenshot](1.png)
![screenshot](2.png)
![screenshot](3.png)
![screenshot](4.png)
![screenshot](5.png)
![screenshot](6.png)
![screenshot](7.png)