# Hands-on Task: Run and Manage a "Hello Web App" (httpd)

## Objective
Deploy and manage a simple Apache-based web server using Kubernetes and:
- ✅ Verify it is running
- ✅ Modify it
- ✅ Scale it
- ✅ Debug it

---

## Prerequisites

Before starting, ensure you have:
- **Kubernetes cluster** running (minikube, Docker Desktop, kind, or cloud-based)
- **kubectl** installed and configured
- **Docker** installed (for building custom images if needed)
- Basic understanding of Kubernetes concepts (Pods, Services, Deployments)

### Check if Kubernetes is running:
```bash
kubectl cluster-info
kubectl get nodes
```

---

## Phase 1: Deploy Apache httpd

### Step 1.1: Create a Simple Pod

Start by running a basic Apache httpd container:

```bash
kubectl run apache-pod \
  --image=httpd:latest \
  
```

**Explanation:**
- `--image=httpd:latest`: Uses official Apache httpd image from Docker Hub
- `--limits`: Maximum resources the pod can consume
- `--requests`: Minimum guaranteed resources

### Step 1.2: Verify Pod is Running

Check the pod status:

```bash
# List all pods
kubectl get pods

# Get detailed pod information
kubectl get pod apache-pod -o wide

# Check pod details
kubectl describe pod apache-pod

# Watch pod status in real-time
kubectl get pods --watch
```

**Expected Output:**
```
NAME          READY   STATUS    RESTARTS   AGE
apache-pod    1/1     Running   0          2m
```

### Step 1.3: Access the Web Application

Forward the port to access Apache locally:

```bash
# Port forward (runs in foreground)
kubectl port-forward pod/apache-pod 8081:80
```

**Expected Output:**
```
Forwarding from 127.0.0.1:8081 -> 80
Forwarding from [::1]:8081 -> 80
```

Now open your browser and visit:
```
http://localhost:8081
```

You should see the default Apache "It works!" page.

### Step 1.4: Test via Command Line

Instead of a browser, test with curl:

```bash
# From another terminal (while port-forward is running)
curl http://localhost:8081

# With verbose output
curl -v http://localhost:8081

# Get response headers only
curl -I http://localhost:8081
```

---

## Phase 2: Modify the Application

### Step 2.1: Create Custom HTML Content

First, create a custom index.html file locally:


# Apply the deployment
kubectl apply -f apache-deployment.yaml
```

### Step 3.3: Verify Deployment

```bash
# List deployments
kubectl get deployments

# List pods created by deployment
kubectl get pods -l app=web

# Watch pod creation in real-time
kubectl get pods -l app=web --watch
```

**Expected Output:**
```
NAME                         READY   STATUS    RESTARTS   AGE
apache-app-5f7b8d9c8-abc12   1/1     Running   0          2m
apache-app-5f7b8d9c8-def45   1/1     Running   0          2m
apache-app-5f7b8d9c8-ghi67   1/1     Running   0          2m
```

### Step 3.4: Create a Service


### Step 3.5: Access the Service

```bash
# Get service details
kubectl get svc apache-service

# Port-forward to the service (if LoadBalancer not available)
kubectl port-forward svc/apache-service 8080:80

# Test the service
curl http://localhost:8080
```

### Step 3.6: Scale Up/Down

Change the number of replicas:

```bash
# Scale to 5 replicas
kubectl scale deployment apache-app --replicas=5

# Scale back down to 2
kubectl scale deployment apache-app --replicas=2

# Watch scaling in action
kubectl get pods -l app=web --watch
```

You can also edit the deployment directly:

```bash
# Opens deployment in your default editor
kubectl edit deployment apache-app
# Change "replicas: 3" to desired number, save and exit
```

---

## Phase 4: Debug the Application

### Step 4.1: Check Pod Logs

```bash
# View logs from a specific pod
kubectl logs apache-app-5f7b8d9c8-abc12

# Follow logs in real-time
kubectl logs -f apache-app-5f7b8d9c8-abc12

# View logs from all pods in the deployment
kubectl logs -l app=web --all-containers=true

# Get last 50 lines of logs
kubectl logs --tail=50 apache-app-5f7b8d9c8-abc12
```

### Step 4.2: Execute Commands Inside Pod

```bash
# Open an interactive shell in a pod
kubectl exec -it apache-app-5f7b8d9c8-abc12 /bin/bash

# Inside the pod, you can run:
# - Check Apache configuration
apache2ctl -V

# - List Apache processes
ps aux | grep apache

# - Check if port 80 is listening
netstat -tlnp | grep 80

# - Verify htdocs content
ls -la /usr/local/apache2/htdocs/
cat /usr/local/apache2/htdocs/index.html

# - Test Apache locally
curl http://localhost:80

# - Exit the pod
exit
```

### Step 4.3: Describe Resources

```bash
# Detailed information about a deployment
kubectl describe deployment apache-app

# Detailed information about a pod
kubectl describe pod apache-app-5f7b8d9c8-abc12

# Detailed information about a service
kubectl describe svc apache-service
```

### Step 4.4: Check Events

```bash
# View cluster events
kubectl get events --sort-by='.lastTimestamp'

# View events for a specific pod
kubectl describe pod apache-app-5f7b8d9c8-abc12 | grep -A 20 Events

# Watch events in real-time
kubectl get events --watch
```

### Step 4.5: Inspect Resource Usage

```bash
# Check CPU and memory usage of nodes
kubectl top nodes

# Check CPU and memory usage of pods
kubectl top pods

# Check resource usage of pods in deployment
kubectl top pods -l app=web

# Get detailed resource requests/limits
kubectl describe deployment apache-app | grep -A 10 "Limits\|Requests"
```

### Step 4.6: Check Pod Status Details

```bash
# Get detailed pod information in YAML format
kubectl get pod apache-app-5f7b8d9c8-abc12 -o yaml

# Check pod conditions
kubectl get pod apache-app-5f7b8d9c8-abc12 -o jsonpath='{.status.conditions[*]}'

# Get only the pod phase (Running, Pending, Failed, etc.)
kubectl get pod apache-app-5f7b8d9c8-abc12 -o jsonpath='{.status.phase}'
```

### Step 4.7: Test Connectivity Between Pods

```bash
# Get a pod name
POD_NAME=$(kubectl get pod -l app=web -o jsonpath='{.items[0].metadata.name}')

# Test if one pod can reach another pod
kubectl exec $POD_NAME -- curl http://localhost:80

# Test if a pod can reach the service
kubectl exec $POD_NAME -- curl http://apache-service:80

# Test DNS resolution
kubectl exec $POD_NAME -- nslookup apache-service
```

### Step 4.8: Common Issues and Fixes

#### Issue: Pod Stuck in Pending

```bash
# Check why pod is pending
kubectl describe pod <pod-name> | grep -A 10 "Events:"

# Common reasons: insufficient resources, node not ready
# Check nodes
kubectl get nodes
kubectl top nodes

# Fix: Reduce resource requests or scale down other pods
```

#### Issue: Pod in CrashLoopBackOff

```bash
# Check the logs to see the error
kubectl logs <pod-name>

# Check previous log if pod crashed
kubectl logs --previous <pod-name>

# Get pod description for more details
kubectl describe pod <pod-name>
```

#### Issue: Pod Can't Access ConfigMap

```bash
# Verify ConfigMap exists and is accessible
kubectl get configmap apache-config
kubectl describe configmap apache-config

# Check if volume mount is correct in pod
kubectl exec <pod-name> -- ls -la /usr/local/apache2/htdocs/

# Recreate ConfigMap if needed
kubectl delete configmap apache-config
kubectl create configmap apache-config --from-file=custom-content/index.html
```

#### Issue: Service Not Accessible

```bash
# Check service endpoints
kubectl get endpoints apache-service

# Check if any pods are selected by the service
kubectl get pods -l app=web

# Verify service selector matches pod labels
kubectl describe svc apache-service | grep Selector

# Check service ports
kubectl get svc apache-service -o yaml | grep -A 5 ports
```

---

## Complete Quick Reference Commands

### Deployment Management
```bash
# Create/Apply resources
kubectl apply -f apache-deployment.yaml
kubectl apply -f apache-service.yaml

# Get resources
kubectl get deployments
kubectl get pods
kubectl get svc

# Scale deployment
kubectl scale deployment apache-app --replicas=5

# Update deployment
kubectl set image deployment/apache-app apache=httpd:2.4

# Delete resources
kubectl delete deployment apache-app
kubectl delete svc apache-service
kubectl delete configmap apache-config
```

### Port Forwarding & Access
```bash
# Port-forward to pod
kubectl port-forward pod/apache-app-xxx 8081:80

# Port-forward to service
kubectl port-forward svc/apache-service 8080:80

# Test with curl
curl http://localhost:8080
```

### Monitoring & Debugging
```bash
# Logs
kubectl logs <pod-name>
kubectl logs -f <pod-name>

# Execute commands
kubectl exec -it <pod-name> /bin/bash

# Describe resources
kubectl describe pod <pod-name>
kubectl describe deployment apache-app

# Resource usage
kubectl top nodes
kubectl top pods

# Events
kubectl get events --sort-by='.lastTimestamp'
```

---

## Cleanup

Remove all resources created during this task:

```bash
# Delete deployment
kubectl delete deployment apache-app

# Delete service
kubectl delete svc apache-service

# Delete ConfigMap
kubectl delete configmap apache-config

# Verify everything is deleted
kubectl get all -l app=web
```

---

## Expected Outcomes

By completing this hands-on task, you should be able to:

✅ **Deploy**: Run Apache httpd on Kubernetes using pods and deployments  
✅ **Verify**: Check pod status, logs, and resource usage  
✅ **Modify**: Update application content using ConfigMaps and direct file transfers  
✅ **Scale**: Create multiple replicas and adjust scaling dynamically  
✅ **Debug**: Troubleshoot issues using logs, exec, describe, and events  

---
## Screenshots
![sc](/Theory/Task-1/1.png)
![sc](/Theory/Task-1/2.png)
![sc](/Theory/Task-1/3.png)
![sc](/Theory/Task-1/4.png)
![sc](/Theory/Task-1/5.png)
![sc](/Theory/Task-1/6.png)
![sc](/Theory/Task-1/7.png)
![sc](/Theory/Task-1/8.png)
![sc](/Theory/Task-1/9.png)
## Notes

- Replace `apache-app-5f7b8d9c8-abc12` with actual pod names from your cluster
- Adjust resource limits/requests based on your cluster capacity
- For production deployments, use Ingress instead of port-forward
- Consider using namespaces to organize resources
- Always use ConfigMaps for configuration management (not kubectl cp)

---

**Happy Kubernetes exploring! 🚀**