package com.epam.gym.workload;

public enum ActionType {
    /** A training was added: its duration counts toward the trainer's month. */
    ADD,
    /** A training was deleted (with its trainee): its duration no longer counts. */
    DELETE
}
