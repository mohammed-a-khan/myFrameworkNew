# Property Standardization Mapping for CS TestForge Framework

## Mapping Table: Old Properties → New Standardized Properties

### Browser Configuration
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `browser.name` | `cs.browser.name` | Keep as primary |
| `browser.default` | `cs.browser.name` | Remove duplicate |
| `browser.headless` | `cs.browser.headless` | Rename |
| `browser.maximize` | `cs.browser.maximize` | Rename |
| `browser.implicit.wait` | `cs.browser.implicit.wait` | Rename |
| `browser.explicit.wait` | `cs.browser.explicit.wait` | Rename |
| `browser.page.load.timeout` | `cs.browser.page.load.timeout` | Rename |
| `browser.reuse.instance` | `cs.browser.reuse.instance` | Rename |

### Application URLs
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `app.base.url` | `cs.app.base.url` | Keep as primary |
| `base.url` | `cs.app.base.url` | Remove duplicate |

### Screenshot Configuration
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `screenshot.on.fail` | `cs.screenshot.on.failure` | Remove |
| `screenshot.on.failure` | `cs.screenshot.on.failure` | Remove |
| `test.screenshot.on.failure` | `cs.screenshot.on.failure` | Remove |
| `cs.screenshot.on.failure` | `cs.screenshot.on.failure` | Keep as primary |
| `screenshot.on.pass` | `cs.screenshot.on.pass` | Rename |
| `screenshot.format` | `cs.screenshot.format` | Keep |

### Test Execution
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `execution.mode` | `cs.execution.mode` | Keep as primary |
| `executionMode` | `cs.execution.mode` | Remove duplicate |
| `thread.count` | `cs.test.thread.count` | Remove |
| `test.thread.count` | `cs.test.thread.count` | Rename |
| `retry.count` | `cs.test.retry.count` | Remove |
| `test.retry.count` | `cs.test.retry.count` | Rename |
| `test.retry.delay` | `cs.test.retry.delay` | Rename |

### Wait Configuration
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `wait.timeout` | `cs.wait.timeout` | Remove |
| `cs.wait.timeout` | `cs.wait.timeout` | Keep |
| `selenium.implicit.wait` | `cs.wait.implicit` | Rename |
| `selenium.pageload.timeout` | `cs.wait.pageload.timeout` | Rename |
| `selenium.script.timeout` | `cs.wait.script.timeout` | Rename |

### Azure DevOps Integration
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `ado.enabled` | `cs.azure.devops.enabled` | Remove |
| `ado.organization` | `cs.azure.devops.organization` | Remove |
| `ado.project` | `cs.azure.devops.project` | Remove |
| `ado.pat` | `cs.azure.devops.token` | Remove |
| `ado.test.plan.id` | `cs.azure.devops.test.plan.id` | Remove |
| `ado.test.suite.id` | `cs.azure.devops.test.suite.id` | Remove |
| `azure.devops.enabled` | `cs.azure.devops.enabled` | Rename |
| `azure.devops.organization` | `cs.azure.devops.organization` | Rename |
| `azure.devops.project` | `cs.azure.devops.project` | Rename |
| `azure.devops.token` | `cs.azure.devops.token` | Keep |

### BDD Configuration
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `cucumber.features.path` | `cs.bdd.features.path` | Remove |
| `cs.feature.path` | `cs.bdd.features.path` | Remove |
| `cs.bdd.features.path` | `cs.bdd.features.path` | Keep |
| `bdd.features.path` | `cs.bdd.features.path` | Remove |
| `cucumber.glue.path` | `cs.bdd.stepdefs.packages` | Remove |
| `cs.step.packages` | `cs.bdd.stepdefs.packages` | Remove |
| `cs.bdd.stepdefs.packages` | `cs.bdd.stepdefs.packages` | Keep |
| `bdd.step.definitions.package` | `cs.bdd.stepdefs.packages` | Remove |

### Database Configuration
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `db.type` | `cs.db.default.type` | Remove |
| `db.host` | `cs.db.default.host` | Remove |
| `db.port` | `cs.db.default.port` | Remove |
| `db.name` | `cs.db.default.name` | Remove |
| `db.username` | `cs.db.default.username` | Remove |
| `db.password` | `cs.db.default.password` | Remove |
| `db.default.type` | `cs.db.default.type` | Rename |
| `db.default.host` | `cs.db.default.host` | Rename |
| `db.default.port` | `cs.db.default.port` | Rename |
| `db.default.name` | `cs.db.default.name` | Rename |
| `db.default.username` | `cs.db.default.username` | Rename |
| `db.default.password` | `cs.db.default.password` | Rename |

### Environment Configuration
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `environment.name` | `cs.environment.name` | Keep as primary |
| `env.current` | `cs.environment.name` | Remove duplicate |

### Report Configuration
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `report.directory` | `cs.report.directory` | Remove |
| `cs.report.directory` | `cs.report.directory` | Keep |
| `report.name` | `cs.report.name` | Rename |
| `report.format` | `cs.report.format` | Rename |

### Video Recording
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `video.recording.enabled` | `cs.video.recording.enabled` | Remove |
| `test.video.recording` | `cs.video.recording.enabled` | Remove |

### AKhan Specific (Keep for backward compatibility)
| Old Property | New Property | Action |
|--------------|--------------|--------|
| `akhan.url` | `cs.akhan.url` | Rename |
| `akhan.environment` | `cs.akhan.environment` | Rename |
| `akhan.user.default` | `cs.akhan.user.default` | Rename |
| `akhan.password.default` | `cs.akhan.password.default` | Rename |

## Implementation Order

### Phase 1: Update Core Configuration Classes
1. CSConfigManager.java - Add property mapping/aliasing support
2. Create property migration utility

### Phase 2: Update Code References
1. Update all Java classes to use new property names
2. Update TestNG suite XML files
3. Update feature files and step definitions

### Phase 3: Clean Application.properties
1. Remove redundant properties
2. Add comments for deprecated properties
3. Create migration guide

### Phase 4: Testing
1. Run all test suites
2. Verify Azure DevOps integration
3. Verify BDD tests
4. Verify database connections

## Backward Compatibility Strategy
- Keep mapping of old to new properties for 1-2 releases
- Log warnings when old properties are used
- Provide migration utility for users