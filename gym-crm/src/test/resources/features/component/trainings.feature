Feature: Adding trainings and telling the Trainer Workload service
  As a trainee
  I want to add trainings with my trainer
  So that they are recorded and the trainer's workload is kept up to date

  Background:
    Given a trainer "coach" registered as "Sardor" "Nazarov" with specialization "Fitness"
    And a trainee "lola" registered as "Lola" "Hamidova"

  Scenario: Adding a training queues an ADD workload message
    When "lola" adds a 60 minute training with "coach" on "2026-11-10" with transaction id "bdd-tx-1"
    Then the response status is 200
    And an "ADD" workload message for "coach" with 60 minutes on "2026-11-10" is queued
    And the message carries a valid service token and transaction id "bdd-tx-1"

  Scenario: Deleting a trainee queues a DELETE for each of their trainings
    Given "lola" added a 60 minute training with "coach" on "2026-11-10"
    And "lola" added a 45 minute training with "coach" on "2026-12-01"
    When "lola" deletes their profile
    Then the response status is 200
    And these workload messages for "coach" are queued:
      | action | date       | minutes |
      | DELETE | 2026-11-10 | 60      |
      | DELETE | 2026-12-01 | 45      |

  Scenario: A user cannot add a training they are not part of
    Given a trainee "stranger" registered as "Akmal" "Begov"
    When "stranger" adds a 60 minute training for "lola" with "coach" on "2026-11-10"
    Then the response status is 403
    And no workload message is queued

  Scenario Outline: An invalid training is rejected and nothing is queued
    When "lola" adds a <minutes> minute training with "coach" on "<date>"
    Then the response status is 400
    And no workload message is queued

    Examples:
      | minutes | date       |
      | 0       | 2026-11-10 |
      | -30     | 2026-11-10 |
      | 60      | not-a-date |
