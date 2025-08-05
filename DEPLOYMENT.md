# CS Framework Deployment Guide

This guide explains how to deploy the CS Test Automation Framework to various Maven repositories.

## Prerequisites

1. Maven 3.6 or higher installed
2. Java 17 or higher
3. Access credentials for your target repository
4. GPG key for signing artifacts (optional, for releases)

## Configuration

### 1. Repository Configuration

Update the `distributionManagement` section in `pom.xml` with your repository URLs:

```xml
<distributionManagement>
    <repository>
        <id>releases</id>
        <url>https://your-nexus-server/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>snapshots</id>
        <url>https://your-nexus-server/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

### 2. Credentials Setup

Copy `settings.xml.template` to `~/.m2/settings.xml` and update with your credentials:

```bash
cp settings.xml.template ~/.m2/settings.xml
```

Edit the file and replace placeholder values with your actual credentials.

## Deployment Options

### Quick Deployment

Use the provided deployment script:

```bash
# Deploy snapshot version
./deploy.sh

# Deploy release version
./deploy.sh --release

# Deploy with specific profile
./deploy.sh --profile nexus

# Skip tests during deployment
./deploy.sh --skip-tests
```

### Manual Deployment

1. **Build and test the framework:**
   ```bash
   mvn clean test
   ```

2. **Generate all artifacts:**
   ```bash
   mvn package javadoc:jar source:jar
   ```

3. **Deploy to repository:**
   ```bash
   mvn deploy
   ```

### Deploying to Different Repositories

#### Nexus Repository

1. Configure Nexus URL in `pom.xml`
2. Add credentials to `settings.xml`
3. Deploy:
   ```bash
   mvn deploy -P nexus
   ```

#### GitHub Packages

1. Update `pom.xml`:
   ```xml
   <distributionManagement>
       <repository>
           <id>github</id>
           <url>https://maven.pkg.github.com/OWNER/REPOSITORY</url>
       </repository>
   </distributionManagement>
   ```

2. Add GitHub token to `settings.xml`:
   ```xml
   <server>
       <id>github</id>
       <username>YOUR_GITHUB_USERNAME</username>
       <password>YOUR_GITHUB_TOKEN</password>
   </server>
   ```

3. Deploy:
   ```bash
   mvn deploy -P github
   ```

#### Maven Central

1. Set up OSSRH account
2. Configure `pom.xml` with OSSRH URLs
3. Add GPG signing configuration
4. Deploy:
   ```bash
   mvn clean deploy -P release
   ```

## Version Management

### Updating Version

```bash
# Update to new version
mvn versions:set -DnewVersion=1.1.0

# Commit version change
mvn versions:commit

# Or revert if needed
mvn versions:revert
```

### Version Numbering

- **SNAPSHOT versions**: For development (e.g., 1.0.0-SNAPSHOT)
- **Release versions**: For stable releases (e.g., 1.0.0)

Follow semantic versioning:
- **Major**: Breaking changes
- **Minor**: New features, backward compatible
- **Patch**: Bug fixes

## Artifacts Generated

The deployment process generates the following artifacts:

1. **Main JAR**: `cs-framework-{version}.jar`
2. **Sources JAR**: `cs-framework-{version}-sources.jar`
3. **JavaDoc JAR**: `cs-framework-{version}-javadoc.jar`
4. **JAR with dependencies**: `cs-framework-{version}-jar-with-dependencies.jar`

## Using the Deployed Framework

Add the framework as a dependency in your test project:

```xml
<dependency>
    <groupId>com.testforge</groupId>
    <artifactId>cs-framework</artifactId>
    <version>1.0.0</version>
</dependency>
```

For snapshot versions:

```xml
<dependency>
    <groupId>com.testforge</groupId>
    <artifactId>cs-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Troubleshooting

### Authentication Issues
- Verify credentials in `settings.xml`
- Check repository permissions
- Ensure server IDs match between `pom.xml` and `settings.xml`

### Build Failures
- Run `mvn clean` before deployment
- Check for compilation errors: `mvn compile`
- Verify tests pass: `mvn test`

### Network Issues
- Check proxy settings in `settings.xml`
- Verify repository URLs are accessible
- Check firewall settings

## Best Practices

1. **Always test before deploying**: Run full test suite
2. **Use semantic versioning**: Follow version conventions
3. **Document changes**: Update CHANGELOG.md
4. **Tag releases**: Create Git tags for releases
5. **Backup**: Keep backups of previous versions
6. **Monitor**: Check deployment logs for issues

## CI/CD Integration

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                sh './deploy.sh --profile nexus'
            }
        }
    }
}
```

### GitHub Actions Example

```yaml
name: Deploy Framework

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Deploy to GitHub Packages
        run: mvn deploy
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Support

For deployment issues:
1. Check the deployment logs
2. Verify all prerequisites are met
3. Consult the Maven documentation
4. Contact the framework team

---

Last updated: 2024