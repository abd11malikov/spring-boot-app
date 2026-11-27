Feature: The trainer's workload follows trainings in Gym CRM
  As a trainer
  I want every training added to or deleted from Gym CRM to be reflected in my monthly workload
  So that my working hours are always correct

  Background:
    Given a trainer "coach" and a trainee "trainee" are registered in Gym CRM

  Scenario: Both services are registered in Eureka
    Then the discovery server lists "GYM-CRM" and "TRAINER-WORKLOAD-SERVICE"

  Scenario: A new training appears in the trainer's monthly workload
    When "trainee" adds a 60 minute training with "coach" on "2026-11-10"
    Then the response status is 200
    And the Trainer Workload service shows 60 minutes for "coach" in 11/2026

  Scenario: Trainings of the same month add up, other months stay separate
    When "trainee" adds a 60 minute training with "coach" on "2026-11-10"
    And "trainee" adds a 45 minute training with "coach" on "2026-11-25"
    And "trainee" adds a 30 minute training with "coach" on "2026-12-02"
    Then the Trainer Workload service shows 105 minutes for "coach" in 11/2026
    And the Trainer Workload service shows 30 minutes for "coach" in 12/2026

  Scenario: Deleting the trainee removes their trainings from the trainer's workload
    Given "trainee" added a 60 minute training with "coach" on "2026-11-10"
    And the Trainer Workload service shows 60 minutes for "coach" in 11/2026
    When "trainee" deletes their profile
    Then the response status is 200
    And the Trainer Workload service shows 0 minutes for "coach" in 11/2026

  Scenario: The transaction id follows a training into the Trainer Workload service
    When "trainee" adds a 60 minute training with "coach" on "2026-11-10" with transaction id "it-trace-1"
    Then the Trainer Workload service shows 60 minutes for "coach" in 11/2026
    And both services logged transaction "it-trace-1"

  Scenario: An invalid training is rejected and never reaches the workload
    When "trainee" adds a 0 minute training with "coach" on "2026-11-10"
    Then the response status is 400
    And the Trainer Workload service has no workload for "coach"

  Scenario: A training added by someone outside it is refused and not counted
    Given a trainee "stranger" is registered in Gym CRM
    When "stranger" adds a 60 minute training for "trainee" with "coach" on "2026-11-10"
    Then the response status is 403
    And the Trainer Workload service has no workload for "coach"
