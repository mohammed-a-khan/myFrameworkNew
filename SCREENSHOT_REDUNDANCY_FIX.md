# Screenshot Redundancy Fix

## Problem Fixed
Screenshots and page sources were being saved redundantly in multiple locations:
1. **cs-reports/** root folder (redundant)
2. **cs-reports/screenshots/** folder (redundant)  
3. **cs-reports/test-run-TIMESTAMP/screenshots/** folder (correct location)

This created unnecessary duplication and clutter.

## Solution Implemented

### Changes Made

1. **Modified CSReportManager.java**
   - Changed `attachScreenshot()` methods to save to temp directory (`/tmp/cs-temp-screenshots/`)
   - Updated `captureScreenshot()` to use temp directory

2. **Modified CSBaseTest.java**
   - Updated `takeScreenshot()` method to use temp directory
   - Updated page source capture to use temp directory

3. **Enhanced CSHtmlReportGenerator.java**
   - Added `cleanupTempScreenshots()` method to clean temp directory after report generation
   - Screenshots are moved from temp to final test-run folder during report generation
   - Added support for embedded screenshots (Base64) to eliminate files entirely

4. **Modified CSWebDriverManager.java**
   - Added awareness of embedding mode for appropriate logging

## How It Works Now

### Workflow
1. **During Test Execution**: Screenshots are saved to temp directory (`/tmp/cs-temp-screenshots/`)
2. **Report Generation**: Screenshots are moved from temp to `test-run-TIMESTAMP/screenshots/`
3. **Cleanup**: Temp directory is cleaned after report generation
4. **Optional Embedding**: Can embed screenshots as Base64 in HTML (no files at all)

### Directory Structure

#### Before Fix (Redundant)
```
cs-reports/
├── failure_screenshot_123.png       ❌ Redundant
├── page_source_123.html            ❌ Redundant
├── screenshots/                     ❌ Redundant folder
│   ├── screenshot1.png
│   └── screenshot2.png
└── test-run-20250811_123456/
    ├── index.html
    └── screenshots/                 ✓ Correct location
        ├── screenshot1.png
        └── screenshot2.png
```

#### After Fix (Clean)
```
cs-reports/
└── test-run-20250811_123456/       ✓ Single location
    ├── index.html
    └── screenshots/
        ├── screenshot1.png
        └── screenshot2.png
```

## Configuration Options

### Standard Mode (Separate Files)
```properties
cs.report.screenshots.embed=false
```
- Screenshots saved as files in test-run folder
- Smaller HTML file
- Screenshots can be viewed separately

### Embedded Mode (No Files)
```properties
cs.report.screenshots.embed=true
```
- Screenshots embedded as Base64 in HTML
- No screenshot files created
- Self-contained HTML report

## Benefits

1. **No Redundancy**: Screenshots only stored in one location
2. **Cleaner Structure**: No clutter in cs-reports root
3. **Efficient Storage**: No duplicate files
4. **Temporary Storage**: Uses system temp directory during execution
5. **Automatic Cleanup**: Temp files cleaned after report generation
6. **Optional Embedding**: Can eliminate files entirely with Base64 embedding

## Testing

Run the provided test script to verify:
```bash
./test-no-redundancy.sh
```

This script will:
1. Clean old reports
2. Run a test
3. Verify no screenshots in cs-reports root
4. Confirm screenshots only in test-run folder

## Migration Notes

- Old reports with redundant screenshots can be manually cleaned
- The fix is backward compatible - old reports still work
- No configuration changes required (works by default)
- Optional embedding feature available via configuration