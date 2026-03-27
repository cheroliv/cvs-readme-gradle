@cucumber @readme @commit
Feature: Commit generated README files

  Background:
    Given a new ReadMe project
    And a "readme.yml" with the following yaml values:
      | key               | value                            |
      | git.token         | <YOUR_GITHUB_PAT>                |
      | git.userName      | test-bot                         |
      | git.userEmail     | test-bot@example.com             |
      | git.commitMessage | chore: generate readme [skip ci] |
    And the git remote validator is mocked with result "TOKEN_PLACEHOLDER"
    And a git repository is initialized

  # ── Groupe 1 — dépendance processReadme ───────────────────────────────────

  Scenario: commitGeneratedReadme triggers processReadme automatically
    Given the commit task is mocked
    And the file "README_truth.adoc" exists with the following content:
      """
      = English
      """
    When I am executing the task "commitGeneratedReadme"
    Then the build should succeed
    And the file "README.adoc" should exist

  Scenario: commitGeneratedReadme generates README.adoc before committing
    Given the commit task is mocked
    And the file "README_truth.adoc" exists with the following content:
      """
      = English
      """
    When I am executing the task "commitGeneratedReadme"
    Then the build should succeed
    And the file "README.adoc" should exist
    And a commit should have been created with message "chore: generate readme [skip ci]"

  # ── Groupe 2 — dépôt propre ───────────────────────────────────────────────

  Scenario: commitGeneratedReadme logs clean repo and creates no commit when nothing changed
    Given the commit task is mocked
    When I am executing the task "commitGeneratedReadme"
    Then the build should succeed
    And the build log should mention a clean repository
    And no commit should have been created

  # ── Groupe 3 — token invalide / placeholder ───────────────────────────────

  Scenario: commitGeneratedReadme fails when token is still a placeholder
    Given the file "README_truth.adoc" exists with the following content:
      """
      = English
      """
    When I am executing the task "commitGeneratedReadme"
    Then the build should fail
    And the build log should contain the following entries:
      | level   | keyword | value       |
      | FAILURE | token   | placeholder |

  Scenario: commitGeneratedReadme fails when token is empty
    Given a "readme.yml" with the following yaml values:
      | key               | value                |
      | git.token         |                      |
      | git.userName      | test-bot             |
      | git.userEmail     | test-bot@example.com |
      | git.commitMessage | chore: test          |
    And the file "README_truth.adoc" exists with the following content:
      """
      = English
      """
    When I am executing the task "commitGeneratedReadme"
    Then the build should fail
    And the build log should contain the following entries:
      | level   | keyword | value       |
      | FAILURE | token   | placeholder |

  # ── Groupe 4 — commit+push mocké ─────────────────────────────────────────

  Scenario: commitGeneratedReadme creates a local commit with the configured message
    Given the commit task is mocked
    And the file "README_truth.adoc" exists with the following content:
      """
      = English
      """
    When I am executing the task "commitGeneratedReadme"
    Then the build should succeed
    And a commit should have been created with message "chore: generate readme [skip ci]"

  Scenario: commitGeneratedReadme logs the list of files to commit
    Given the commit task is mocked
    And the file "README_truth.adoc" exists with the following content:
      """
      = English
      """
    When I am executing the task "commitGeneratedReadme"
    Then the build should succeed
    And the build log should contain the following entries:
      | level | keyword  | value    |
      | INFO  | Fichiers | commiter |

  Scenario: commitGeneratedReadme logs that push is skipped in mock mode
    Given the commit task is mocked
    And the file "README_truth.adoc" exists with the following content:
      """
      = English
      """
    When I am executing the task "commitGeneratedReadme"
    Then the build should succeed
    And the build log should contain the following entries:
      | level | keyword | value   |
      | INFO  | mock    | skipped |

  Scenario: commitGeneratedReadme does not commit when repo is clean in mock mode
    Given the commit task is mocked
    When I am executing the task "commitGeneratedReadme"
    Then the build should succeed
    And the build log should mention a clean repository
    And no commit should have been created

  # ── Groupe 5 — pas de .git ────────────────────────────────────────────────
  # Le Background initialise un .git — ce scénario le supprime avant l'exécution.

  Scenario: commitGeneratedReadme fails when no git repository exists
    Given the git repository is deleted
    And the commit task is mocked
    And the file "README_truth.adoc" exists with the following content:
      """
      = English
      """
    When I am executing the task "commitGeneratedReadme"
    Then the build should fail
    And the build log should contain the following entries:
      | level   | keyword    | value |
      | FAILURE | repository | not   |
