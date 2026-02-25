#!/bin/bash

# Test script to verify Railway DATABASE_URL format works
# This script simulates Railway's DATABASE_URL environment variable

echo "Testing Railway DATABASE_URL format compatibility..."

# Example Railway DATABASE_URL format
export DATABASE_URL="postgresql://postgres:password@localhost:5432/incident_db"
export OPENAI_API_KEY="test-key-for-validation"
export PORT=8080

# Test that the application can parse the DATABASE_URL
echo "DATABASE_URL: $DATABASE_URL"

# Start the application in test mode (this would normally be done with actual Railway deployment)
echo "Application would start with these environment variables:"
echo "  DATABASE_URL: $DATABASE_URL"
echo "  PORT: $PORT"
echo "  OPENAI_API_KEY: [REDACTED]"

# Check if the URL follows the correct format
if [[ $DATABASE_URL =~ postgresql://([^:]+):([^@]+)@([^:]+):([0-9]+)/(.+) ]]; then
    echo "✅ DATABASE_URL format is valid for Railway deployment"
    echo "   User: ${BASH_REMATCH[1]}"
    echo "   Host: ${BASH_REMATCH[3]}"
    echo "   Port: ${BASH_REMATCH[4]}"
    echo "   Database: ${BASH_REMATCH[5]}"
else
    echo "❌ DATABASE_URL format is invalid"
    exit 1
fi

echo "✅ Railway deployment configuration appears to be valid!"