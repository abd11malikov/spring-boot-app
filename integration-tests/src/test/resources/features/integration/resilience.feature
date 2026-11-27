Feature: Gym CRM keeps working when the other side is down
  As a gym user
  I want to keep adding trainings when the Trainer Workload service has a problem
  And to get a clear answer when an update can't be recorded
  So that no training is lost or counted wrongly

  Background:
    Given a trainer "coach" and a trainee "trainee" are registered in Gym CRM

  Scenario: Trainings added while the Trainer Workload service is down are counted when it is back
    Given the Trainer Workload service is stopped
    When "trainee" adds a 60 minute training with "coach" on "2026-11-10"
    Then the response status is 200
    When the Trainer Workload service is started again
    Then the Trainer Workload service shows 60 minutes for "coach" in 11/2026

  Scenario: While the message broker is down, trainings are refused and nothing is counted
    Given the message broker is stopped
    When "trainee" adds a 60 minute training with "coach" on "2026-11-10"
    Then the response status is 503
    And the error message is "Trainer workload updates are temporarily unavailable, please try again later"
    When the message broker is started again
    And "trainee" adds a 45 minute training with "coach" on "2026-11-12"
    Then the response status is 200
    And the Trainer Workload service shows 45 minutes for "coach" in 11/2026
    And the trainings of "trainee" in Gym CRM add up to 45 minutes
