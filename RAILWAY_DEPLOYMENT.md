# Cloud Deployment Guide

This document provides instructions for deploying the Incident Management API to cloud platforms like Railway and Render.

## Prerequisites

1. Railway account: [Sign up at railway.app](https://railway.app)
2. Railway CLI installed (optional): `npm install -g @railway/cli`

## Deployment Steps

### 1. Connect Repository to Railway

1. Log in to [Railway](https://railway.app)
2. Click "New Project" → "Deploy from GitHub repo"
3. Select your incident-management-api repository
4. Railway will automatically detect the Dockerfile and start building

### 2. Add Database Service

1. In your Railway project, click "New Service"
2. Select "Database" → "PostgreSQL"
3. Railway will automatically create a PostgreSQL instance and set the `DATABASE_URL` environment variable

**Important Note**: Cloud providers (Railway, Render) provide `DATABASE_URL` in the format `postgresql://user:pass@host:port/db`, but Spring Boot expects `jdbc:postgresql://...`. The application includes automatic conversion logic (`DatabaseUrlConverter`) to handle this for seamless deployment.

### 3. Configure Environment Variables

Add the following environment variables in Railway dashboard:

#### Required Variables
```bash
OPENAI_API_KEY=your_openai_api_key_here
```

#### Optional Production Variables (with defaults)
```bash
# Database Configuration (automatically handled by Railway's DATABASE_URL)
DB_POOL_SIZE=10
DB_POOL_MIN_IDLE=2

# Application Configuration
LOG_LEVEL=INFO
JPA_DDL_AUTO=update
SWAGGER_ENABLED=true

# OpenAI Configuration
OPENAI_MODEL=gpt-4o-mini
OPENAI_TEMPERATURE=0.3
```

### 4. Health Check Configuration

The application includes health check endpoints:
- Health check URL: `/actuator/health`
- Railway will automatically use this for health monitoring

### 5. Access Your Application

Once deployed, your application will be available at:
- **API Base URL**: `https://your-app-name.up.railway.app`
- **Swagger UI**: `https://your-app-name.up.railway.app/swagger-ui.html`
- **Health Check**: `https://your-app-name.up.railway.app/actuator/health`

## Environment Variable Reference

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DATABASE_URL` | PostgreSQL connection string | Auto-set by Railway | ✅ |
| `OPENAI_API_KEY` | OpenAI API key for AI analysis | - | ✅ |
| `PORT` | Application port | 8080 | ❌ |
| `JPA_DDL_AUTO` | Hibernate DDL mode | update | ❌ |
| `LOG_LEVEL` | Application log level | INFO | ❌ |
| `SWAGGER_ENABLED` | Enable/disable Swagger UI | true | ❌ |

## Features Available After Deployment

1. **Full REST API** with all incident management endpoints
2. **Interactive Swagger Documentation** at `/swagger-ui.html`
3. **AI-Powered Analysis** for automatic incident categorization
4. **Similarity Search** to find related incidents
5. **Health Monitoring** via actuator endpoints
6. **Seed Data** automatically loaded on first startup

## API Endpoints

Once deployed, you can access:

- `GET /api/incidents` - List all incidents
- `POST /api/incidents` - Create new incident (with AI analysis)
- `GET /api/incidents/{id}` - Get incident by ID
- `PATCH /api/incidents/{id}/status` - Update incident status
- `GET /api/incidents/similar?description=...` - Find similar incidents
- `GET /api/incidents/metrics` - Get incident metrics

## Troubleshooting

### Common Issues

1. **Database Connection Issues**
   - Ensure PostgreSQL service is running in Railway
   - Check that `DATABASE_URL` is automatically set

2. **OpenAI API Issues**
   - Verify `OPENAI_API_KEY` is correctly set
   - Check API key has sufficient quota

3. **Application Won't Start**
   - Check logs in Railway dashboard
   - Verify health check endpoint responds

### Logs Access

View application logs in Railway dashboard:
1. Go to your project
2. Click on your service
3. Navigate to "Logs" tab

### Manual Health Check

Test the deployment:
```bash
curl https://your-app-name.up.railway.app/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

## Security Notes

- Environment variables are securely managed by Railway
- Application runs as non-root user in container
- Error details are hidden in production
- Database credentials are automatically managed

## Scaling

Railway automatically handles:
- Load balancing
- SSL certificates
- CDN distribution
- Container scaling based on traffic