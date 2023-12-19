package ru.stradiavanti.train_plan_bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "trainers")
public class Trainer {

    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String specialization;
    private String photoPath;
    public Long getTrainerId() {
        return id;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getPhotoPath() {
        return photoPath;
    }
    public String getSpecialization() {
        return specialization;
    }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", specialization=" + specialization + '\'' +
                ", photoPath='" + photoPath + '\'' +
                '}';
    }
}
