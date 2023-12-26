package ru.stradiavanti.train_plan_bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "trainers")
@Table(name = "trainers", schema = "public")
public class Trainer {
  @Id
  @SequenceGenerator(

    name = "trainer_sequence",
    sequenceName = "trainer_sequence",
    allocationSize = 1

  )
  @GeneratedValue(
    strategy = GenerationType.IDENTITY
  )
  @Column(
    name = "id",
    updatable = false,
    nullable = false
  )
  private Long id;

  private String firstName;
  private String fatherName;
  private String lastName;
  private String specialization;
  private String imageUrl;

  private Boolean isFree;

  private Double rating;
  private Integer numberOfVotes;
  @Override
  public String toString() {
    return "Trainer{" +
      "id=" + id +
      ", firstName='" + firstName + '\'' +
      ", lastName='" + lastName + '\'' +
      ", specialization='" + specialization + '\'' +
      ", imageUrl='" + imageUrl + '\'' +
      '}';
  }
}
