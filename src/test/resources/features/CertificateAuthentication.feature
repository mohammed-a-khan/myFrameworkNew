@api @certificate @authentication
Feature: API Certificate Authentication
  As a test engineer
  I want to test API endpoints with client certificate authentication
  So that I can verify secure API access with certificates

  Background:
    Given the certificate directory is "certificates"
    And the default certificate password is "badssl.com"

  @smoke @cert-loading
  Scenario: Load and validate client certificate
    Given I have a client certificate "badssl.com-client.p12"
    When I load the certificate with password "badssl.com"
    Then the certificate should be loaded successfully
    And the certificate should be valid
    And the certificate should not be expired

  @api-call @cert-auth
  Scenario: Make API call with client certificate
    Given I have a client certificate "badssl.com-client.p12"
    And I load the certificate with password "badssl.com"
    When I make a GET request to "https://client.badssl.com/"
    Then the response status should be 200
    And the response should contain "client"
    And the response should indicate successful authentication

  @multiple-formats
  Scenario Outline: Test different certificate formats
    Given I have a client certificate "<certificate_file>"
    When I load the certificate with password "<password>"
    Then the certificate should be loaded successfully
    
    Examples:
      | certificate_file         | password    |
      | badssl.com-client.p12   | badssl.com  |
      | badssl.com-client.pfx   | badssl.com  |
      | client.pfx              | badssl.com  |

  @error-handling @negative
  Scenario: Handle invalid certificate password
    Given I have a client certificate "badssl.com-client.p12"
    When I try to load the certificate with wrong password "invalid_password"
    Then the certificate loading should fail
    And the error message should contain "password"

  @error-handling @negative
  Scenario: Handle non-existent certificate file
    Given I have a client certificate "non_existent.pfx"
    When I try to load the certificate with password "any_password"
    Then the certificate loading should fail
    And the error message should contain "not found"

  @performance @caching
  Scenario: Verify certificate caching
    Given I have a client certificate "badssl.com-client.p12"
    When I load the certificate with password "badssl.com" for the first time
    And I measure the loading time
    And I load the same certificate again
    Then the second load should be faster due to caching
    When I clear the certificate cache
    And I load the certificate again
    Then a new SSL context should be created

  @mutual-tls
  Scenario: Test mutual TLS authentication
    Given I have a client certificate "badssl.com-client.p12"
    And the server requires client certificate authentication
    When I establish a mutual TLS connection to "https://client-cert.badssl.com/"
    Then the connection should be established successfully
    And the server should accept the client certificate
    And the response should be authorized

  @certificate-chain
  Scenario: Validate certificate chain
    Given I have a client certificate with chain "badssl.com-client.pem"
    When I load the certificate chain
    Then all certificates in the chain should be valid
    And the certificate chain should be properly ordered
    And the root CA should be trusted

  @concurrent @load-test
  Scenario: Handle concurrent certificate operations
    Given I have a client certificate "badssl.com-client.p12"
    When I load the certificate concurrently from 10 threads
    Then all threads should successfully load the certificate
    And there should be no concurrency issues
    And the cache should handle concurrent access properly