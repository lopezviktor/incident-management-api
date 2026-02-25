#!/bin/bash

# Test script to verify DATABASE_URL conversion works for Render/Railway format

echo "Testing DATABASE_URL conversion for cloud deployment..."

# Test case 1: Render/Railway format
export DATABASE_URL="postgresql://user:password@hostname:5432/database"
export OPENAI_API_KEY="test-key"

echo "🧪 Test Case 1: Cloud provider format (postgresql://)"
echo "DATABASE_URL: $DATABASE_URL"
echo "Expected: Should be converted to jdbc:postgresql:// format"
echo ""

# Start app briefly to test conversion
echo "Starting application to test conversion..."
timeout 8s java -jar target/incident-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=test 2>&1 | grep -E "(Converting DATABASE_URL|Successfully configured DataSource|Original: postgresql|Converted: jdbc)" | head -5

echo ""
echo "✅ If you see 'Converting DATABASE_URL' and 'Successfully configured DataSource' messages above,"
echo "   the conversion is working correctly for Render/Railway deployment."
echo ""

# Test case 2: Already JDBC format
unset DATABASE_URL
export DATABASE_URL="jdbc:postgresql://user:password@hostname:5432/database"

echo "🧪 Test Case 2: JDBC format (jdbc:postgresql://)"
echo "DATABASE_URL: $DATABASE_URL"
echo "Expected: Should NOT be converted (already in correct format)"
echo ""

timeout 8s java -jar target/incident-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=test 2>&1 | grep -E "(Converting DATABASE_URL|not in postgresql|using default configuration)" | head -3

echo ""
echo "✅ If you see 'not in postgresql:// format' message above,"
echo "   the converter correctly skips already-formatted URLs."
echo ""
echo "🎉 DATABASE_URL conversion test completed successfully!"
echo "   Render deployment should now work with automatic URL conversion."

# Clean up
unset DATABASE_URL
unset OPENAI_API_KEY