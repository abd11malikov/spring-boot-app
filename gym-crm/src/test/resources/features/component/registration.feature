Feature: Registering trainees and trainers
  As a new gym member or coach
  I want to register without having an account
  So that I get generated credentials and can use the system right away

  Scenario: A trainee registers and gets credentials and a token
    When a trainee registers with first name "Aziz" and last name "Karimov"
    Then the response status is 201
    And the response has a username starting with "Aziz.Karimov"
    And the response has a 10 character password
    And the token from the response opens the trainee's profile

  Scenario: A trainer registers with an existing specialization
    When a trainer registers with first name "Malika" and last name "Yusupova" and specialization "Yoga"
    Then the response status is 201
    And the response has a username starting with "Malika.Yusupova"

  Scenario: Registering the same name twice gives the second user a serial number
    Given a trainee "first" registered as "Dilshod" "Rakhimov"
    When a trainee registers with first name "Dilshod" and last name "Rakhimov"
    Then the response status is 201
    And the new username is the username of "first" followed by a serial number

  Scenario Outline: Registration without a required field is rejected
    When a trainee registers with the body <body>
    Then the response status is 400
    And the response names the invalid field "<field>"

    Examples:
      | body                         | field     |
      | {"lastName":"Karimov"}       | firstName |
      | {"firstName":"Aziz"}         | lastName  |
      | {"firstName":" ","lastName":"Karimov"} | firstName |

  Scenario: A trainer cannot register with an unknown specialization
    When a trainer registers with first name "Rustam" and last name "Aliyev" and specialization id 999
    Then the response status is 404
