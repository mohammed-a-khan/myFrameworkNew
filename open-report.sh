#!/bin/bash

# Find and open the latest test report

REPORT_DIR="cs-reports"
LATEST_REPORT=""

# Check if latest-report.html exists
if [ -f "$REPORT_DIR/latest-report.html" ]; then
    echo "Opening latest report..."
    # Extract the actual report path from the redirect
    REPORT_PATH=$(grep -oP 'url=\K[^"]+' "$REPORT_DIR/latest-report.html" 2>/dev/null || echo "")
    if [ -n "$REPORT_PATH" ]; then
        LATEST_REPORT="$REPORT_DIR/$REPORT_PATH"
    fi
fi

# If not found, look for the most recent test-run directory
if [ -z "$LATEST_REPORT" ] || [ ! -f "$LATEST_REPORT" ]; then
    LATEST_DIR=$(ls -dt "$REPORT_DIR"/test-run-* 2>/dev/null | head -1)
    if [ -n "$LATEST_DIR" ]; then
        LATEST_REPORT="$LATEST_DIR/index.html"
    fi
fi

# Check if report exists
if [ -f "$LATEST_REPORT" ]; then
    echo "========================================="
    echo "Test Report Location:"
    echo "$LATEST_REPORT"
    echo "========================================="
    
    # Get absolute path
    FULL_PATH=$(realpath "$LATEST_REPORT")
    echo "Full path: $FULL_PATH"
    
    # Try to open in browser (Windows/WSL)
    if command -v wslview &> /dev/null; then
        wslview "$FULL_PATH"
    elif command -v xdg-open &> /dev/null; then
        xdg-open "$FULL_PATH"
    elif command -v open &> /dev/null; then
        open "$FULL_PATH"
    else
        echo "Please open manually: file://$FULL_PATH"
    fi
else
    echo "No test report found!"
    echo "Run tests first using: ./run-tests.sh"
fi