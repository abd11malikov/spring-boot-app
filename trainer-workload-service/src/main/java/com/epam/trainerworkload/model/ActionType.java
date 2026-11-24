package com.epam.trainerworkload.model;

public enum ActionType {
    /** A training was planned: its duration is added to the trainer's month. */
    ADD,
    /** A training was cancelled: its duration is taken away from the trainer's month. */
    DELETE
}
