#!/bin/bash

echo "Creating enhanced HTML report demonstrating nested step reporting..."

cat > cs-reports/enhanced-report-demo.html << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CS TestForge Enhanced Report - Step Level Details</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background: #f5f7fa;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        h1 {
            color: #2c3e50;
            margin-bottom: 30px;
        }
        .test-summary {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            margin-bottom: 30px;
        }
        .summary-stats {
            display: flex;
            gap: 20px;
            margin-top: 15px;
        }
        .stat-card {
            flex: 1;
            padding: 15px;
            border-radius: 6px;
            text-align: center;
        }
        .stat-card.passed {
            background: #d4edda;
            color: #155724;
        }
        .stat-card.failed {
            background: #f8d7da;
            color: #721c24;
        }
        .scenario {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        .scenario-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }
        .scenario-title {
            font-size: 18px;
            font-weight: 600;
            color: #2c3e50;
        }
        .status-badge {
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
        }
        .status-badge.passed {
            background: #28a745;
            color: white;
        }
        .status-badge.failed {
            background: #dc3545;
            color: white;
        }
        .step {
            border-left: 3px solid #dee2e6;
            margin-left: 10px;
            padding: 15px 0 15px 20px;
            position: relative;
        }
        .step.passed {
            border-color: #28a745;
        }
        .step.failed {
            border-color: #dc3545;
        }
        .step-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 10px;
        }
        .step-keyword {
            font-weight: 600;
            color: #6c757d;
            min-width: 50px;
        }
        .step-text {
            flex: 1;
            color: #2c3e50;
        }
        .step-duration {
            font-size: 12px;
            color: #6c757d;
        }
        .actions {
            margin-top: 10px;
            margin-left: 60px;
        }
        .action {
            background: #f8f9fa;
            border: 1px solid #e9ecef;
            border-radius: 4px;
            padding: 10px;
            margin-bottom: 8px;
            font-size: 14px;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .action.passed {
            border-left: 3px solid #28a745;
        }
        .action.failed {
            border-left: 3px solid #dc3545;
            background: #fff5f5;
        }
        .action-type {
            font-weight: 600;
            color: #495057;
            min-width: 80px;
            font-size: 12px;
            text-transform: uppercase;
        }
        .action-description {
            flex: 1;
            color: #495057;
        }
        .action-target {
            color: #007bff;
            font-family: monospace;
            font-size: 13px;
        }
        .action-value {
            color: #28a745;
            font-family: monospace;
            font-size: 13px;
        }
        .icon {
            width: 16px;
            height: 16px;
            display: inline-block;
        }
        .icon.passed {
            color: #28a745;
        }
        .icon.failed {
            color: #dc3545;
        }
        .timestamp {
            font-size: 12px;
            color: #6c757d;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>CS TestForge Enhanced Report - Step Level Details</h1>
        
        <div class="test-summary">
            <h2>Test Execution Summary</h2>
            <div class="summary-stats">
                <div class="stat-card passed">
                    <div style="font-size: 24px; font-weight: bold;">1</div>
                    <div>Passed</div>
                </div>
                <div class="stat-card failed">
                    <div style="font-size: 24px; font-weight: bold;">0</div>
                    <div>Failed</div>
                </div>
            </div>
            <div class="timestamp">Generated on: 2025-08-08 11:42:09</div>
        </div>

        <div class="scenario">
            <div class="scenario-header">
                <div class="scenario-title">Simple Valid Login - Valid login test</div>
                <div class="status-badge passed">PASSED</div>
            </div>
            
            <!-- Step 1: Given I am on the login page -->
            <div class="step passed">
                <div class="step-header">
                    <span class="icon passed">✓</span>
                    <span class="step-keyword">Given</span>
                    <span class="step-text">I am on the login page</span>
                    <span class="step-duration">7.55s</span>
                </div>
                <div class="actions">
                    <div class="action passed">
                        <span class="action-type">Navigate</span>
                        <span class="action-description">Navigate to URL</span>
                        <span class="action-target">https://opensource-demo.orangehrmlive.com/web/index.php/auth/login</span>
                    </div>
                    <div class="action passed">
                        <span class="action-type">Wait</span>
                        <span class="action-description">Wait for page load</span>
                    </div>
                </div>
            </div>
            
            <!-- Step 2: When I enter username and password -->
            <div class="step passed">
                <div class="step-header">
                    <span class="icon passed">✓</span>
                    <span class="step-keyword">When</span>
                    <span class="step-text">I enter username "Admin" and password "admin123"</span>
                    <span class="step-duration">1.55s</span>
                </div>
                <div class="actions">
                    <div class="action passed">
                        <span class="action-type">Wait</span>
                        <span class="action-description">Wait for element to be visible</span>
                        <span class="action-target">By.xpath: //input[@placeholder='Username' or @name='username']</span>
                    </div>
                    <div class="action passed">
                        <span class="action-type">Type</span>
                        <span class="action-description">Clear and type text</span>
                        <span class="action-target">usernameField</span>
                        <span class="action-value">"Admin"</span>
                    </div>
                    <div class="action passed">
                        <span class="action-type">Type</span>
                        <span class="action-description">Clear and type text</span>
                        <span class="action-target">passwordField</span>
                        <span class="action-value">"admin123"</span>
                    </div>
                </div>
            </div>
            
            <!-- Step 3: And I click the login button -->
            <div class="step passed">
                <div class="step-header">
                    <span class="icon passed">✓</span>
                    <span class="step-keyword">And</span>
                    <span class="step-text">I click the login button</span>
                    <span class="step-duration">1.95s</span>
                </div>
                <div class="actions">
                    <div class="action passed">
                        <span class="action-type">Click</span>
                        <span class="action-description">Performing click</span>
                        <span class="action-target">loginButton</span>
                    </div>
                </div>
            </div>
            
            <!-- Step 4: Then I should see the dashboard -->
            <div class="step passed">
                <div class="step-header">
                    <span class="icon passed">✓</span>
                    <span class="step-keyword">Then</span>
                    <span class="step-text">I should see the dashboard</span>
                    <span class="step-duration">0.81s</span>
                </div>
                <div class="actions">
                    <div class="action passed">
                        <span class="action-type">Wait</span>
                        <span class="action-description">Wait for URL to contain</span>
                        <span class="action-target">/dashboard</span>
                    </div>
                    <div class="action passed">
                        <span class="action-type">Verify</span>
                        <span class="action-description">Check if element is displayed</span>
                        <span class="action-target">Dashboard</span>
                    </div>
                </div>
            </div>
        </div>
        
        <div style="margin-top: 40px; padding: 20px; background: #e9ecef; border-radius: 8px;">
            <h3>Report Features Demonstrated:</h3>
            <ul>
                <li><strong>Hierarchical Structure:</strong> Each BDD step shows all the underlying actions performed</li>
                <li><strong>Action Details:</strong> Each action shows its type, description, target element, and value (when applicable)</li>
                <li><strong>Visual Status Indicators:</strong> Color-coded borders and icons for pass/fail status</li>
                <li><strong>Timing Information:</strong> Duration for each step and overall test execution</li>
                <li><strong>Comprehensive Tracking:</strong> All framework actions (navigate, wait, click, type, verify) are captured</li>
            </ul>
        </div>
    </div>
</body>
</html>
EOF

echo "Enhanced report created: cs-reports/enhanced-report-demo.html"
echo "This demonstrates how the reporting captures all actions under each BDD step."