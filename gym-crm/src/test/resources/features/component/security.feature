Feature: Logging in and out
  As a registered user
  I want to log in with my credentials and log out again
  So that only I can use my account

  Background:
    Given a trainee "nodir" registered as "Nodir" "Sobirov"

  Scenario: Logging in with the right password returns a working token
    When "nodir" logs in with the right password
    Then the response status is 200
    And the response contains a bearer token
    And the token from the response opens the profile of "nodir"

  Scenario: Logging in with a wrong password is refused
    When "nodir" logs in with the password "wrong-password"
    Then the response status is 401
    And the error message is "Invalid username or password"

  Scenario: Three wrong passwords block the user, even for the right password
    Given "nodir" failed to log in 3 times
    When "nodir" logs in with the right password
    Then the response status is 429
    And the error message is "User is temporarily blocked after too many unsuccessful logins, try again later"

  Scenario: A logged-out token no longer works
    When "nodir" logs out
    Then the response status is 200
    And the old token of "nodir" is rejected with "Token has been revoked by logout"

  Scenario: Protected endpoints need a token
    When an anonymous user requests the training types
    Then the response status is 401
    And the error message is "Missing bearer token"
