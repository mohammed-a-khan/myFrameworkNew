# Screenshot Embedding Feature

## Overview
Implemented a new feature to control how screenshots are stored in test reports. Users can now choose between:
1. **External Mode (default)**: Screenshots saved as separate files in a `screenshots/` folder
2. **Embedded Mode**: Screenshots embedded directly in the HTML report as Base64 data URIs

## Configuration

### Property Setting
Add or modify the following property in `resources/config/application.properties`:

```properties
# Embed screenshots in HTML (true) or keep as separate files (false)
cs.report.screenshots.embed=false
```

### Runtime Override
You can override this setting at runtime using system properties:

```bash
# Enable embedding
mvn test -Dcs.report.screenshots.embed=true

# Disable embedding (default)
mvn test -Dcs.report.screenshots.embed=false
```

## Implementation Details

### New Components

1. **CSImageUtils.java** - Utility class for image processing
   - `imageToBase64DataUri()`: Converts image files to Base64 data URIs
   - `isDataUri()`: Checks if a string is a data URI
   - `getFileSize()`: Returns human-readable file size

2. **Enhanced CSHtmlReportGenerator.java**
   - Added `embedScreenshotsAsBase64()` method to embed screenshots
   - Modified report generation logic to handle both modes
   - Added cleanup for temporary screenshot files when embedding

3. **Updated CSWebDriverManager.java**
   - Added awareness of embedding mode when saving screenshots
   - Logs differently based on embedding mode

## Benefits

### External Mode (Default)
- **Pros**:
  - Smaller HTML file size
  - Screenshots can be viewed/downloaded separately
  - Better for reports with many screenshots
  - Traditional approach, familiar to users
  
- **Cons**:
  - Requires managing separate screenshot files
  - Report needs the screenshots folder to display images
  - More files to archive/transfer

### Embedded Mode
- **Pros**:
  - Single self-contained HTML file
  - No external dependencies
  - Easy to share/email report
  - No broken image links
  
- **Cons**:
  - Larger HTML file size
  - Can't download screenshots separately
  - May be slower to load with many screenshots

## File Structure

### External Mode
```
cs-reports/
└── test-run-20250811_011010/
    ├── index.html (smaller file)
    ├── report-data.json
    └── screenshots/
        ├── screenshot_001.png
        ├── screenshot_002.png
        └── screenshot_003.png
```

### Embedded Mode
```
cs-reports/
└── test-run-20250811_011010/
    ├── index.html (larger file, contains Base64 images)
    └── report-data.json
```

## Usage Examples

### Command Line
```bash
# Run with external screenshots (default)
mvn test -DsuiteXmlFile=suites/your-test.xml

# Run with embedded screenshots
mvn test -DsuiteXmlFile=suites/your-test.xml -Dcs.report.screenshots.embed=true
```

### Properties File Configuration
```properties
# In application.properties
cs.report.screenshots.embed=true  # Always embed
cs.report.screenshots.embed=false # Never embed (default)
```

## Testing

Use the provided test scripts to verify the feature:

```bash
# Test both modes
./test-screenshot-embedding.sh

# Simple embedding test
./simple-embed-test.sh
```

## Notes

1. The feature automatically cleans up temporary screenshot files when embedding is enabled
2. System properties override file-based configuration
3. The HTML report's JavaScript handles both embedded and external images seamlessly
4. Base64 encoding increases the HTML size by approximately 33% compared to binary image size

## Future Enhancements

1. Add compression before Base64 encoding to reduce size
2. Implement lazy loading for embedded images
3. Add option to embed only failed test screenshots
4. Provide a converter tool to switch between modes for existing reports