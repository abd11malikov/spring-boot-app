Feature: Reading trainers' workload over REST
  As a client holding a service token
  I want to read a trainer's monthly training minutes
  So that I can report on trainers' workload

  Background:
    Given trainer "Rustam.Aliyev" named "Rustam" "Aliyev" has trained 60 minutes on "2026-11-10"
    And trainer "Rustam.Aliyev" named "Rustam" "Aliyev" has trained 30 minutes on "2026-11-20"

  Scenario: Reading a trainer's summary by year and month
    When the summary of "Rustam.Aliyev" is requested with a service token
    Then the response status is 200
    And the summary shows 90 minutes in 11/2026

  Scenario: Reading one month
    When the workload of "Rustam.Aliyev" in 11/2026 is requested with a service token
    Then the response status is 200
    And the monthly workload is 90 minutes

  Scenario: A month without trainings has no minutes
    When the workload of "Rustam.Aliyev" in 3/2026 is requested with a service token
    Then the response status is 200
    And the monthly workload is 0 minutes

  Scenario: Searching trainers by first and last name
    When trainers named "Rustam" "Aliyev" are searched with a service token
    Then the response status is 200
    And the search finds "Rustam.Aliyev"

  Scenario: An unknown trainer has no workload
    When the summary of "Nobody.Here" is requested with a service token
    Then the response status is 404

  Scenario: An invalid month is rejected
    When the workload of "Rustam.Aliyev" in 13/2026 is requested with a service token
    Then the response status is 400

  Scenario: Reading without a service token is refused
    When the summary of "Rustam.Aliyev" is requested without a token
    Then the response status is 401
