Feature: Keeping trainers' monthly workload from Gym CRM messages
  As the Trainer Workload service
  I want to process every training Gym CRM adds or cancels
  So that each trainer's monthly training minutes stay correct

  Background:
    Given a trainer "Nodira.Karimova" named "Nodira" "Karimova"

  Scenario: The first training creates the trainer's workload document
    Given the trainer "Nodira.Karimova" has no workload yet
    When Gym CRM reports an ADD of 60 minutes on "2026-11-10" for "Nodira.Karimova"
    Then the workload of "Nodira.Karimova" has these months:
      | year | month | minutes |
      | 2026 | 11    | 60      |
    And "Nodira.Karimova" is stored as "Nodira" "Karimova" with status active

  Scenario: Trainings add up per month and year
    When Gym CRM reports an ADD of 60 minutes on "2026-11-10" for "Nodira.Karimova"
    And Gym CRM reports an ADD of 45 minutes on "2026-11-20" for "Nodira.Karimova"
    And Gym CRM reports an ADD of 30 minutes on "2026-12-01" for "Nodira.Karimova"
    And Gym CRM reports an ADD of 90 minutes on "2027-01-05" for "Nodira.Karimova"
    Then the workload of "Nodira.Karimova" has these months:
      | year | month | minutes |
      | 2026 | 11    | 105     |
      | 2026 | 12    | 30      |
      | 2027 | 1     | 90      |

  Scenario: A cancelled training is taken off its month
    Given Gym CRM reported an ADD of 60 minutes on "2026-11-10" for "Nodira.Karimova"
    And Gym CRM reported an ADD of 30 minutes on "2026-11-12" for "Nodira.Karimova"
    When Gym CRM reports a DELETE of 60 minutes on "2026-11-10" for "Nodira.Karimova"
    Then the workload of "Nodira.Karimova" has these months:
      | year | month | minutes |
      | 2026 | 11    | 30      |

  Scenario: Cancelling the only training of a month removes the month
    Given Gym CRM reported an ADD of 60 minutes on "2026-11-10" for "Nodira.Karimova"
    When Gym CRM reports a DELETE of 60 minutes on "2026-11-10" for "Nodira.Karimova"
    Then the workload of "Nodira.Karimova" has no months

  Scenario: Cancelling a training of an unknown trainer stores nothing
    Given the trainer "Nodira.Karimova" has no workload yet
    When Gym CRM reports a DELETE of 60 minutes on "2026-11-10" for "Nodira.Karimova"
    Then after the message is processed the trainer "Nodira.Karimova" still has no workload

  Scenario: A message without a service token goes to the dead letter queue
    When a workload message for "Nodira.Karimova" arrives without a service token
    Then the message is moved to the dead letter queue with reason "Missing service token"
    And the trainer "Nodira.Karimova" still has no workload

  Scenario: A message signed with someone else's key goes to the dead letter queue
    When a workload message for "Nodira.Karimova" arrives signed with a foreign key
    Then the message is moved to the dead letter queue with reason "Invalid service token"
    And the trainer "Nodira.Karimova" still has no workload

  Scenario Outline: A message missing required information goes to the dead letter queue
    When a workload message arrives with the body <body>
    Then the message is moved to the dead letter queue with a reason starting with "<reason>"
    And the trainer "Nodira.Karimova" still has no workload

    Examples:
      | body                                                                                                     | reason              |
      | {"trainerUsername":"Nodira.Karimova","trainingDuration":60}                                               | Invalid fields:     |
      | {"trainerUsername":"Nodira.Karimova","trainerFirstName":"Nodira","trainerLastName":"Karimova","isActive":true,"trainingDate":"2026-11-10","trainingDuration":0,"actionType":"ADD"} | Invalid fields: trainingDuration |
      | {"trainerUsername":"Nodira.Karimova","trainerFirstName":"Nodira","trainerLastName":"Karimova","isActive":true,"trainingDate":"2026-11-10","trainingDuration":60,"actionType":"CANCEL"} | Malformed JSON body |
      | not json at all                                                                                          | Malformed JSON body |
