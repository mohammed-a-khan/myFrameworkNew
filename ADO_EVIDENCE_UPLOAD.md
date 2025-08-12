# Azure DevOps Test Evidence Upload

## Feature Implementation
Added functionality to automatically zip and upload the complete test report folder to Azure DevOps test runs as an attachment.

## What Gets Uploaded

The system now uploads the entire test execution report folder as a single zip file containing:
- `index.html` - Complete HTML test report
- `report-data.json` - Test execution data
- `screenshots/` - All captured screenshots from the test run
- Any other test artifacts generated during execution

## How It Works

### 1. Test Report Generation
Your framework generates test reports in the `cs-reports/test-run-<timestamp>/` folder with:
```
cs-reports/
└── test-run-20250812_205729/
    ├── index.html
    ├── report-data.json
    └── screenshots/
        ├── test1_screenshot.png
        ├── test2_screenshot.png
        └── ...
```

### 2. Evidence Collection
When the test run completes, the `CSEvidenceUploader`:
1. Finds the latest `test-run-*` folder in `cs-reports/`
2. Creates a zip archive of the entire folder
3. Names it `test-evidence-<runId>-<timestamp>.zip`

### 3. Upload to Azure DevOps
The zip file is uploaded as an attachment to the test run using the Azure DevOps REST API:
- Endpoint: `/test/runs/{runId}/attachments`
- Type: GeneralAttachment
- Comment: "Complete Test Evidence"

### 4. Access in Azure DevOps
1. Navigate to your test run in Azure DevOps
2. Go to the "Attachments" tab
3. Download the `test-evidence-*.zip` file
4. Extract to view complete test execution evidence

## Implementation Details

### New Methods Added

#### `CSEvidenceUploader.uploadTestReportFolder()`
- Finds latest test report folder
- Creates zip archive
- Uploads to test run
- Returns attachment ID

#### `CSEvidenceUploader.createZipFromFolder()`
- Recursively zips folder contents
- Preserves folder structure
- Handles nested directories

#### `CSEvidenceUploader.uploadFileToTestRun()`
- Uploads file directly to test run (not individual test result)
- Encodes file as Base64
- Creates attachment in Azure DevOps

### Integration Point
The upload happens in `CSTestRunManager.completeTestRun()`:
```java
// Upload complete test report folder as zip before completing
if (config.isUploadAttachments()) {
    logger.info("Uploading complete test report folder to test run...");
    CompletableFuture<String> reportUpload = 
        evidenceUploader.uploadTestReportFolder(currentTestRun.id);
    // Wait for upload with timeout
}
```

## Configuration

Controlled by the existing setting in `application.properties`:
```properties
ado.upload.attachments=true
```

## Benefits

1. **Complete Evidence Package**: All test artifacts in one downloadable zip
2. **Preserved Structure**: Maintains original folder structure
3. **HTML Reports**: Full HTML reports viewable after download
4. **Screenshots**: All screenshots captured during test execution
5. **Historical Reference**: Complete test run evidence for audit/review

## Verification

After running tests:
1. Check Azure DevOps test run
2. Navigate to Attachments tab
3. Look for `test-evidence-<runId>-<timestamp>.zip`
4. Download and extract to verify contents

## Troubleshooting

If attachments are not appearing:
- Check logs for "Uploading complete test report folder"
- Verify `ado.upload.attachments=true` in configuration
- Ensure PAT token has sufficient permissions
- Check that `cs-reports/` folder exists and contains test runs

## File Size Limits
- Azure DevOps has attachment size limits (typically 100MB)
- Large test runs with many screenshots may need compression optimization
- Consider cleanup of old report folders to save space