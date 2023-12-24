package ru.stradiavanti.train_plan_bot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {
  Optional<Trainer> findByLastNameAndFirstNameAndFatherName(
    String lastName,
    String firstName,
    String fatherName
  );
}
