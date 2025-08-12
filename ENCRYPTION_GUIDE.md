# CS Framework Encryption/Decryption Feature

## Overview
The CS Framework now includes automatic encryption and decryption capabilities for sensitive data in configuration files, feature files, and test data files. This feature ensures that sensitive information like passwords, API keys, and other credentials are never stored in plain text.

## Key Features
- **Automatic Decryption**: Values encrypted with `ENC()` wrapper are automatically decrypted when accessed
- **AES-256-GCM Encryption**: Uses strong, industry-standard encryption
- **Transparent Integration**: Works seamlessly with existing framework components
- **Multiple Data Source Support**: Works with properties files, feature files, Excel, and CSV files

## How It Works

### Encryption Format
Encrypted values are wrapped in `ENC()` format:
```
password=ENC(encrypted_base64_string_here)
```

### Automatic Decryption Locations
1. **Properties Files** - CSConfigManager automatically decrypts values
2. **Feature Files** - BDD step parameters are automatically decrypted
3. **Excel Files** - Cell values are decrypted when read
4. **CSV Files** - Field values are decrypted when read

## Setting Up Encryption

### 1. Generate an Encryption Key
```bash
java -cp target/classes com.testforge.cs.security.CSEncryptionUtils genkey
```
This generates a secure 256-bit key.

### 2. Set the Encryption Key
Set the generated key as an environment variable:
```bash
export CS_ENCRYPTION_KEY=your_generated_key_here
```

Or as a system property:
```bash
-Dcs.encryption.key=your_generated_key_here
```

### 3. Encrypt Sensitive Values
```bash
java -cp target/classes com.testforge.cs.security.CSEncryptionUtils encrypt "your_password_here"
```
This returns the encrypted value wrapped in `ENC()`.

## Usage Examples

### In Properties Files
```properties
# application.properties
app.username=testuser
app.password=ENC(rBNjimFZqSyLKj8/zcqX2g==)
api.key=ENC(xY3Nk9Lb2PqRst5/abcdef==)
database.password=ENC(mNpQr7StUvWx8/hijklmn==)
```

### In Feature Files
```gherkin
Scenario: Login with encrypted credentials
  Given I am on the login page
  When I enter username "admin"
  And I enter password "ENC(rBNjimFZqSyLKj8/zcqX2g==)"
  Then I should be logged in
```

### In Excel Test Data
Create test data with encrypted values:
| Username | Password | Expected |
|----------|----------|----------|
| admin | ENC(rBNjimFZqSyLKj8/zcqX2g==) | success |
| user1 | ENC(xY3Nk9Lb2PqRst5/abcdef==) | success |

### In CSV Test Data
```csv
username,password,expected
admin,ENC(rBNjimFZqSyLKj8/zcqX2g==),success
user1,ENC(xY3Nk9Lb2PqRst5/abcdef==),success
```

## Code Examples

### Accessing Encrypted Properties
```java
// Automatic decryption when accessing properties
String password = CSConfigManager.getInstance().getProperty("app.password");
// 'password' now contains the decrypted value
```

### Manual Encryption/Decryption
```java
import com.testforge.cs.security.CSEncryptionUtils;

// Encrypt a value
String encrypted = CSEncryptionUtils.encrypt("sensitive_data");
System.out.println(encrypted); // ENC(encrypted_base64_string)

// Decrypt a value
String decrypted = CSEncryptionUtils.decrypt("ENC(encrypted_base64_string)");
System.out.println(decrypted); // sensitive_data

// Check if a value is encrypted
boolean isEncrypted = CSEncryptionUtils.isEncrypted("ENC(abc123)");

// Process text with multiple encrypted values
String text = "User: admin, Pass: ENC(abc123), Key: ENC(def456)";
String processed = CSEncryptionUtils.processEncryptedValues(text);
// All ENC() values in the text are decrypted
```

## Security Best Practices

### 1. Key Management
- **Never commit encryption keys to version control**
- Use environment variables or secure key management systems
- Rotate keys periodically
- Use different keys for different environments

### 2. Environment-Specific Keys
```bash
# Development
export CS_ENCRYPTION_KEY_DEV=dev_key_here

# Testing
export CS_ENCRYPTION_KEY_TEST=test_key_here

# Production
export CS_ENCRYPTION_KEY_PROD=prod_key_here
```

### 3. CI/CD Integration
Store encryption keys as secure environment variables in your CI/CD pipeline:
- **Jenkins**: Use Credentials Plugin
- **GitHub Actions**: Use Secrets
- **Azure DevOps**: Use Variable Groups with encryption

### 4. Key Rotation
To rotate keys:
1. Generate a new key
2. Re-encrypt all values with the new key
3. Update the key in all environments
4. Remove the old key after verification

## Troubleshooting

### Common Issues

#### 1. Decryption Fails
**Symptom**: Original encrypted value is returned instead of decrypted value
**Solution**: 
- Check if encryption key is set correctly
- Verify the encrypted value format (must be wrapped in `ENC()`)
- Check logs for decryption errors

#### 2. Wrong Key Error
**Symptom**: `javax.crypto.AEADBadTagException`
**Solution**: The encryption key doesn't match the key used to encrypt the value

#### 3. Key Not Found
**Symptom**: Warning about using default key
**Solution**: Set the `CS_ENCRYPTION_KEY` environment variable or system property

### Debug Logging
Enable debug logging to troubleshoot encryption issues:
```xml
<logger name="com.testforge.cs.security" level="DEBUG"/>
```

## Command-Line Utility

The framework includes a command-line utility for encryption operations:

```bash
# Show help
java -cp target/classes com.testforge.cs.security.CSEncryptionUtils

# Encrypt a value
java -cp target/classes com.testforge.cs.security.CSEncryptionUtils encrypt "password123"

# Decrypt a value
java -cp target/classes com.testforge.cs.security.CSEncryptionUtils decrypt "ENC(encrypted_string)"

# Generate a new key
java -cp target/classes com.testforge.cs.security.CSEncryptionUtils genkey
```

## Integration with Test Automation

### Example: Secure Login Test
```java
@Test
public void testSecureLogin() {
    // Password is automatically decrypted from properties
    String username = config.getProperty("test.username");
    String password = config.getProperty("test.password"); // Decrypted automatically
    
    loginPage.login(username, password);
    assertTrue(dashboardPage.isDisplayed());
}
```

### Example: API Test with Encrypted Token
```java
@Test
public void testAPIWithEncryptedToken() {
    // API token is automatically decrypted
    String apiToken = config.getProperty("api.token"); // Decrypted automatically
    
    Response response = given()
        .header("Authorization", "Bearer " + apiToken)
        .when()
        .get("/api/users");
    
    assertEquals(200, response.getStatusCode());
}
```

## Migration Guide

### Converting Existing Plain Text Values

1. **Identify Sensitive Data**
   - Passwords
   - API keys
   - Database credentials
   - OAuth tokens
   - Any other sensitive configuration

2. **Encrypt Each Value**
   ```bash
   # For each sensitive value
   java -cp target/classes com.testforge.cs.security.CSEncryptionUtils encrypt "current_plain_text_value"
   ```

3. **Update Configuration Files**
   Replace plain text values with encrypted versions:
   ```properties
   # Before
   database.password=plaintext123
   
   # After
   database.password=ENC(rBNjimFZqSyLKj8/zcqX2g==)
   ```

4. **Test the Migration**
   Run your test suite to ensure all encrypted values are properly decrypted

## Performance Considerations

- Decryption is performed on-demand and cached where possible
- Initial decryption adds minimal overhead (< 1ms per value)
- No performance impact for non-encrypted values
- Bulk operations (Excel/CSV) decrypt values as they're read

## Compliance and Auditing

This encryption feature helps with:
- **PCI DSS** - Protecting cardholder data
- **GDPR** - Securing personal data
- **HIPAA** - Protecting health information
- **SOC 2** - Demonstrating security controls

## Future Enhancements

Planned improvements:
- [ ] Support for HashiCorp Vault integration
- [ ] AWS KMS integration
- [ ] Azure Key Vault integration
- [ ] Automatic key rotation
- [ ] Encryption for entire files
- [ ] Support for different encryption algorithms

## Support

For issues or questions about the encryption feature:
1. Check the troubleshooting section above
2. Enable debug logging for detailed information
3. Contact the framework team with specific error messages

## License

This encryption feature is part of the CS TestForge Framework and follows the same licensing terms.