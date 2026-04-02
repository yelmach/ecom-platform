# OCI Manual Deployment Guide

This guide explains how to deploy `ecom-platform` manually to an Oracle Cloud Infrastructure (OCI) VM before automating it with Jenkins.

The goal is simple:

- create a free-tier VM
- prepare the server
- run the app with Docker Compose
- access the app from a browser

This is the manual deployment path that should work first. Jenkins comes later and automates the same flow.

## 1. Architecture Used In OCI

This project is deployed on a single OCI VM using Docker Compose.

The VM runs:

- `mongo`
- `minio`
- `discovery-service`
- `user-service`
- `product-service`
- `media-service`
- `gateway-service`
- `frontend`

The frontend is exposed on `https://<VM_PUBLIC_IP>:4200`.
The gateway is exposed on `https://<VM_PUBLIC_IP>:8443`.

## 2. OCI Resources To Create

### Compute

Recommended for this learning project:

- Shape: `VM.Standard.A1.Flex`
- OCPU: `4`
- Memory: `24 GB`
- OS: Oracle Linux

This is enough for a demo deployment of this stack and fits OCI Always Free if available in your home region.

### Networking

Create:

- one VCN
- one public subnet
- one public IP for the VM


## 3. OCI Ingress Rules

Open these ports in your OCI security list or network security group:

- `22` for SSH
- `4200` for frontend
- `8443` for gateway
- `8761` for Eureka optional debugging
- `9000` for MinIO API optional debugging
- `9001` for MinIO Console optional debugging

For a normal demo, the important public ports are:

- `4200`
- `8443`

## 4. Connect To The VM

Use the private key you downloaded from OCI:

```bash
chmod 600 ~/Downloads/your-private-key.key
ssh -i ~/Downloads/your-private-key.key opc@<VM_PUBLIC_IP>
```

If SSH says the key is too open, fix the permissions with `chmod 600`.

## 5. Install Docker On Oracle Linux

Oracle Linux often defaults to Podman. This project is built around Docker, so install Docker Engine explicitly.

```bash
sudo dnf remove -y podman podman-docker buildah runc docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine
sudo dnf install -y dnf-plugins-core git openssl
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker opc
```

Then reconnect to the VM:

```bash
exit
ssh -i ~/Downloads/your-private-key.key opc@<VM_PUBLIC_IP>
```

Verify Docker:

```bash
docker --version
docker compose version
sudo systemctl status docker
```

## 6. Clone The Repository

```bash
cd ~
git clone https://github.com/yelmach/ecom-platform.git
cd ecom-platform
```

## 7. Prepare Environment Variables

Create the deployment env file from the example:

```bash
cp backend/docker.env.example backend/docker.env
```

Edit it:

```bash
nano backend/docker.env
```

At minimum, review and update:

- `MONGO_PASSWORD`
- `MINIO_SECRET_KEY`
- `GATEWAY_SSL_KEY_STORE_PASSWORD`
- `MEDIA_PUBLIC_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

Example VM-oriented values:

```env
MEDIA_PUBLIC_BASE_URL=https://<VM_PUBLIC_IP>:8443/ecom-media
CORS_ALLOWED_ORIGINS=https://localhost:4200,http://localhost:4200,https://<VM_PUBLIC_IP>:4200
```

Important:

- `GATEWAY_SSL_KEY_STORE_PASSWORD` must exactly match the password used when creating `gateway.p12`

## 8. Generate Gateway TLS Certificates

Create the cert directory:

```bash
mkdir -p backend/certs
```

Generate the gateway TLS cert and key:

```bash
openssl req -x509 -nodes -newkey rsa:2048 -sha256 -days 825 \
  -keyout backend/certs/gateway.key \
  -out backend/certs/gateway.crt \
  -subj "/CN=<VM_PUBLIC_IP>" \
  -addext "subjectAltName=IP:<VM_PUBLIC_IP>,DNS:localhost,IP:127.0.0.1"
```

Generate the PKCS12 keystore:

```bash
openssl pkcs12 -export \
  -out backend/certs/gateway.p12 \
  -inkey backend/certs/gateway.key \
  -in backend/certs/gateway.crt \
  -name gateway \
  -passout pass:changeit
```

If you use `changeit`, then `backend/docker.env` must contain:

```env
GATEWAY_SSL_KEY_STORE_PASSWORD=changeit
```

## 9. Generate JWT RSA Keys

The `user-service` signs JWTs with the private key.
The `gateway-service` verifies JWTs with the public key.

Create the keys:

```bash
mkdir -p backend/keys
openssl genpkey -algorithm RSA -out backend/keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl pkey -in backend/keys/private.pem -pubout -out backend/keys/public.pem
```

These two files must stay as a matching pair.

## 10. Start The Application

From the repo root:

```bash
docker compose --env-file backend/docker.env -f docker-compose.yml up --build -d
```

Or using the Makefile:

```bash
make prod-up
```

Note:

- `make prod-up` runs attached
- the direct `docker compose ... -d` command is more convenient on the VM

## 11. Check Container Status

List running containers:

```bash
docker ps
```

Check full logs:

```bash
docker compose --env-file backend/docker.env -f docker-compose.yml logs -f
```

Check a specific service:

```bash
docker compose --env-file backend/docker.env -f docker-compose.yml logs -f gateway-service
docker compose --env-file backend/docker.env -f docker-compose.yml logs -f user-service
docker compose --env-file backend/docker.env -f docker-compose.yml logs -f media-service
```

## 12. Test The Deployment

From a browser:

- Frontend: `https://<VM_PUBLIC_IP>:4200`
- Gateway health: `https://<VM_PUBLIC_IP>:8443/actuator/health`
- Eureka: `http://<VM_PUBLIC_IP>:8761`

Because the certificate is self-signed, the browser will show a warning. Accept it for testing.

## 13. Useful Runtime Commands

Start:

```bash
docker compose --env-file backend/docker.env -f docker-compose.yml up --build -d
```

Stop:

```bash
docker compose --env-file backend/docker.env -f docker-compose.yml down
```

Stop and remove volumes:

```bash
docker compose --env-file backend/docker.env -f docker-compose.yml down -v
```

Restart one service:

```bash
docker compose --env-file backend/docker.env -f docker-compose.yml restart gateway-service
```

Rebuild one service:

```bash
docker compose --env-file backend/docker.env -f docker-compose.yml up --build -d gateway-service
```

## 14. Manual Deployment Checklist

Use this checklist before moving to Jenkins:

1. The VM is reachable by SSH
2. Docker and Docker Compose are installed
3. OCI ports are open
4. `backend/docker.env` is configured for the VM
5. `backend/certs` contains `gateway.crt`, `gateway.key`, and `gateway.p12`
6. `backend/keys` contains `private.pem` and `public.pem`
7. `docker compose ... up --build -d` completes successfully
8. Frontend opens in the browser
9. Login/register works through the browser
10. Authenticated endpoints work through the gateway

## 15. Common Problems And Fixes

### Problem: `docker.service does not exist`

Cause:

- Docker Engine is not installed
- the VM is still using Podman defaults

Fix:

- install Docker Engine using the commands in section 5

### Problem: Docker tries to use `podman.sock`

Cause:

- the `docker` command points to Podman instead of Docker Engine

Fix:

- remove Podman wrappers
- install Docker Engine
- reconnect to the VM after adding the user to the `docker` group

### Problem: `Permission denied` while creating `backend/certs/gateway.key`

Cause:

- `backend/certs` has the wrong owner

Fix:

```bash
sudo chown -R "$USER":"$USER" backend/certs
chmod 755 backend/certs
```

### Problem: gateway fails with `keystore password was incorrect`

Cause:

- `GATEWAY_SSL_KEY_STORE_PASSWORD` does not match the password used to create `gateway.p12`

Fix:

- recreate `gateway.p12` with the correct password
- or update `backend/docker.env` so the password matches

### Problem: Postman works but browser login/register gives `Forbidden`

Cause:

- CORS origins do not include the VM browser origin

Fix:

- set `CORS_ALLOWED_ORIGINS` in `backend/docker.env`

Example:

```env
CORS_ALLOWED_ORIGINS=https://localhost:4200,http://localhost:4200,https://<VM_PUBLIC_IP>:4200
```

## 16. Why Manual Deployment Matters

Manual deployment is the foundation for CI/CD.

Before Jenkins, you need proof that:

- the app can run on a real server
- the environment variables are correct
- the certs and keys are correct
- the network and ports are correct

Once manual deployment is stable, Jenkins can automate the same steps safely.

