#!/bin/bash

# CS Framework Deployment Script
# This script builds and deploys the framework to a Maven repository

echo "=================================="
echo "CS Framework Deployment Script"
echo "=================================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Parse command line arguments
PROFILE=""
SKIP_TESTS=false
RELEASE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --release)
            RELEASE=true
            shift
            ;;
        --help)
            echo "Usage: ./deploy.sh [options]"
            echo "Options:"
            echo "  --profile <profile>  Maven profile to activate"
            echo "  --skip-tests         Skip running tests"
            echo "  --release           Deploy as release (not snapshot)"
            echo "  --help              Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Clean and compile
echo "Cleaning and compiling project..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "Error: Compilation failed"
    exit 1
fi

# Run tests unless skipped
if [ "$SKIP_TESTS" = false ]; then
    echo "Running tests..."
    mvn test
    
    if [ $? -ne 0 ]; then
        echo "Error: Tests failed"
        exit 1
    fi
else
    echo "Skipping tests..."
fi

# Generate JavaDoc
echo "Generating JavaDoc..."
mvn javadoc:javadoc javadoc:jar

if [ $? -ne 0 ]; then
    echo "Warning: JavaDoc generation failed"
fi

# Package the framework
echo "Packaging framework..."
mvn package

if [ $? -ne 0 ]; then
    echo "Error: Packaging failed"
    exit 1
fi

# Generate source JAR
echo "Generating source JAR..."
mvn source:jar

if [ $? -ne 0 ]; then
    echo "Warning: Source JAR generation failed"
fi

# Deploy to repository
echo "Deploying to repository..."

DEPLOY_CMD="mvn deploy"

if [ -n "$PROFILE" ]; then
    DEPLOY_CMD="$DEPLOY_CMD -P$PROFILE"
fi

if [ "$SKIP_TESTS" = true ]; then
    DEPLOY_CMD="$DEPLOY_CMD -DskipTests"
fi

if [ "$RELEASE" = true ]; then
    DEPLOY_CMD="$DEPLOY_CMD -P release"
fi

echo "Executing: $DEPLOY_CMD"
$DEPLOY_CMD

if [ $? -eq 0 ]; then
    echo "=================================="
    echo "Deployment completed successfully!"
    echo "=================================="
    
    # Display artifact information
    echo ""
    echo "Artifact Information:"
    echo "Group ID: com.testforge"
    echo "Artifact ID: cs-framework"
    echo "Version: $(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)"
    echo ""
    echo "Generated artifacts:"
    ls -la target/*.jar
else
    echo "Error: Deployment failed"
    exit 1
fi