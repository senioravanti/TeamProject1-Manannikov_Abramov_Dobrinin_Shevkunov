package ru.stradiavanti.train_plan_bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalTime;

@Data
@Entity(name = "schedule")
public class Schedule {
    @Id
    private Long clientId;

    private Long trainerId;

    private String[] trainingDays;

    private String[] trainingTimes;
}
