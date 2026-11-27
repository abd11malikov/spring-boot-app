package com.epam.trainerworkload.repository;

import com.epam.trainerworkload.MongoTestContainer;
import com.epam.trainerworkload.config.MongoConfig;
import com.epam.trainerworkload.model.TrainerWorkload;
import jakarta.validation.ConstraintViolationException;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The repository against a real MongoDB: search and update by username, search by name, indexes, validation
 * and optimistic locking.
 */
@DataMongoTest
@ActiveProfiles("test")
@Import({MongoTestContainer.class, MongoConfig.class})
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class TrainerWorkloadRepositoryTest {

    @Autowired
    private TrainerWorkloadRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void findByUsername_returnsSavedDocumentWithNestedSummary() {
        // given
        TrainerWorkload workload = workload("Nodira.Karimova", "Nodira", "Karimova");
        workload.addTraining(LocalDate.of(2026, 11, 10), 60);
        repository.save(workload);

        // when
        TrainerWorkload found = repository.findByUsername("Nodira.Karimova").orElseThrow();

        // then
        assertEquals("Nodira", found.getFirstName());
        assertTrue(found.isActive());
        assertEquals(60, found.getMonthlyDuration(2026, 11));
    }

    @Test
    void documentShape_matchesTheSchema() {
        // given
        TrainerWorkload workload = workload("Nodira.Karimova", "Nodira", "Karimova");
        workload.addTraining(LocalDate.of(2026, 11, 10), 60);
        repository.save(workload);

        // when
        Document stored = mongoTemplate.getCollection("trainer_workloads").find().first();

        // then
        assertEquals("Nodira.Karimova", stored.getString("username"));
        assertEquals("Karimova", stored.getString("lastName"));
        assertEquals(Boolean.TRUE, stored.getBoolean("status"));
        Document year = stored.getList("years", Document.class).get(0);
        Document month = year.getList("months", Document.class).get(0);
        assertEquals(2026, year.getInteger("year"));
        assertEquals(11, month.getInteger("month"));
        assertEquals(60L, month.getLong("trainingSummaryDuration"));
    }

    @Test
    void save_foundDocument_updatesItInPlace() {
        // given
        repository.save(workload("Nodira.Karimova", "Nodira", "Karimova"));
        TrainerWorkload found = repository.findByUsername("Nodira.Karimova").orElseThrow();

        // when
        found.addTraining(LocalDate.of(2026, 11, 10), 45);
        repository.save(found);

        // then
        assertEquals(1, repository.count());
        assertEquals(45, repository.findByUsername("Nodira.Karimova").orElseThrow().getMonthlyDuration(2026, 11));
    }

    @Test
    void findByFirstNameAndLastName_returnsOnlyMatchingTrainers() {
        // given
        repository.save(workload("Nodira.Karimova", "Nodira", "Karimova"));
        repository.save(workload("Nodira.Karimova1", "Nodira", "Karimova"));
        repository.save(workload("Nodira.Aliyeva", "Nodira", "Aliyeva"));

        // when
        List<TrainerWorkload> found = repository.findByFirstNameAndLastName("Nodira", "Karimova");

        // then
        assertEquals(List.of("Nodira.Karimova", "Nodira.Karimova1"),
                found.stream().map(TrainerWorkload::getUsername).sorted().toList());
    }

    @Test
    void indexes_onUsernameAndOnName_areCreated() {
        // when
        List<IndexInfo> indexes = mongoTemplate.indexOps(TrainerWorkload.class).getIndexInfo();

        // then
        IndexInfo username = index(indexes, "trainer_username_idx");
        assertTrue(username.isUnique());
        assertEquals(List.of("username"), username.getIndexFields().stream().map(field -> field.getKey()).toList());
        assertEquals(List.of("firstName", "lastName"), index(indexes, "trainer_name_idx").getIndexFields().stream()
                .map(field -> field.getKey()).toList());
    }

    @Test
    void sameUsernameTwice_isRejectedByUniqueIndex() {
        // given
        repository.save(workload("Nodira.Karimova", "Nodira", "Karimova"));

        // when / then
        assertThrows(DuplicateKeyException.class,
                () -> repository.save(workload("Nodira.Karimova", "Other", "Trainer")));
    }

    @Test
    void invalidDocument_isRejectedBeforeSaving() {
        // when
        assertThrows(ConstraintViolationException.class, () -> repository.save(workload(" ", "Nodira", null)));

        // then
        assertEquals(0, repository.count());
    }

    @Test
    void staleCopy_cannotOverwriteNewerVersion() {
        // given
        repository.save(workload("Nodira.Karimova", "Nodira", "Karimova"));
        TrainerWorkload first = repository.findByUsername("Nodira.Karimova").orElseThrow();
        TrainerWorkload second = repository.findByUsername("Nodira.Karimova").orElseThrow();
        first.addTraining(LocalDate.of(2026, 11, 10), 60);
        repository.save(first);

        // when / then
        second.addTraining(LocalDate.of(2026, 11, 11), 30);
        assertThrows(OptimisticLockingFailureException.class, () -> repository.save(second));
    }

    private static IndexInfo index(List<IndexInfo> indexes, String name) {
        return indexes.stream().filter(index -> index.getName().equals(name)).findFirst().orElseThrow();
    }

    private static TrainerWorkload workload(String username, String firstName, String lastName) {
        return new TrainerWorkload(username, firstName, lastName, true);
    }
}
