Feature: Managing profiles
  As a trainee
  I want to view and change my profile
  So that my data stays correct, while nobody else can change it

  Background:
    Given a trainee "kamola" registered as "Kamola" "Ergasheva"
    And a trainee "bekzod" registered as "Bekzod" "Tursunov"

  Scenario: A trainee updates their own profile
    When "kamola" changes their name to "Kamila" "Ergasheva" and address to "Bukhara"
    Then the response status is 200
    And the profile of "kamola" shows first name "Kamila" and address "Bukhara"

  Scenario: A trainee cannot update someone else's profile
    When "bekzod" tries to change the profile of "kamola"
    Then the response status is 403
    And the error message is "You can only access your own profile"

  Scenario: Any logged-in user can view a profile
    When "bekzod" views the profile of "kamola"
    Then the response status is 200

  Scenario: Viewing a trainee that does not exist
    When "bekzod" views the trainee profile "Nobody.Here"
    Then the response status is 404
