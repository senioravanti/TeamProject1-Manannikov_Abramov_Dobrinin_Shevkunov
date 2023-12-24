package ru.stradiavanti.train_plan_bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;

// Аннотация говорит о том, что данный класс нужно привязать к таблице, параметром
// аннотации является имя таблицы.
// Поля это столбцы таблицы, экземпляры этого класса -> строки таблицы

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "clients")
@Table(name = "clients", schema = "public")
public class User {

  // Объявляем свойство primary key, он обязательно должен иметь тип Long
  @Id
  private Long chatId;
  private Long trainerId;

  private String firstName;
  private String lastName;
  private LocalDate startSubscriptionDate;
  private LocalDate endSubscriptionDate;

  @Override
  public String toString() {
    return "User{" +
      "chatId=" + chatId +
      ", firstName='" + firstName + '\'' +
      ", lastName='" + lastName + '\'' +
      ", trainerId=" + trainerId +
      '}';
  }
}
