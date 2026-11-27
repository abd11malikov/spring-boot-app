package com.epam.trainerworkload.repository;

import com.epam.trainerworkload.model.TrainerWorkload;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Trainer workload documents in MongoDB. Search is by username (unique index); an update is a {@link #save} of a
 * document found that way. The name search uses the {@code firstName + lastName} index.
 */
public interface TrainerWorkloadRepository extends MongoRepository<TrainerWorkload, String> {

    Optional<TrainerWorkload> findByUsername(String username);

    List<TrainerWorkload> findByFirstNameAndLastName(String firstName, String lastName);
}
