#!/bin/bash

echo "Testing screenshot capture and base64 validation"

# Set encryption key for tests
export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

# Clean up any old test files
rm -f /tmp/test_screenshot.png /tmp/decoded_screenshot.png

# Run a simple test to capture and validate screenshots
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 30s mvn test -Dsurefire.suiteXmlFiles=suites/simple-threadlocal-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -q

# Check if any HTML reports were generated
report_file=$(find target/surefire-reports -name "*.html" -type f 2>/dev/null | head -1)

if [[ -n "$report_file" ]]; then
    echo "Found HTML report: $report_file"
    
    # Extract first base64 image from the report
    base64_data=$(grep -o 'data:image/[^;]*;base64,[^"]*' "$report_file" | head -1)
    
    if [[ -n "$base64_data" ]]; then
        echo "Found base64 image data in report"
        
        # Extract just the base64 part (after "base64,")
        pure_base64=$(echo "$base64_data" | sed 's/.*base64,//')
        
        echo "Base64 length: ${#pure_base64}"
        echo "First 50 chars: ${pure_base64:0:50}"
        
        # Try to decode and validate the base64
        echo "$pure_base64" | base64 -d > /tmp/decoded_screenshot.png 2>/dev/null
        
        if file /tmp/decoded_screenshot.png | grep -i "PNG image data"; then
            echo "✓ Base64 data is VALID PNG image"
            echo "Image details:"
            file /tmp/decoded_screenshot.png
        elif file /tmp/decoded_screenshot.png | grep -i "JPEG image data"; then
            echo "✓ Base64 data is VALID JPEG image"
            echo "Image details:"
            file /tmp/decoded_screenshot.png
        else
            echo "✗ Base64 data is NOT a valid image"
            echo "File type detected:"
            file /tmp/decoded_screenshot.png
            echo "First few bytes (hex):"
            xxd -l 16 /tmp/decoded_screenshot.png 2>/dev/null || echo "Cannot read decoded file"
        fi
    else
        echo "✗ No base64 image data found in HTML report"
        echo "Report contains these image references:"
        grep -i "img\|image" "$report_file" | head -5
    fi
else
    echo "✗ No HTML reports found"
    ls -la target/surefire-reports/ 2>/dev/null || echo "No surefire-reports directory"
fi

# Clean up
rm -f /tmp/test_screenshot.png /tmp/decoded_screenshot.png