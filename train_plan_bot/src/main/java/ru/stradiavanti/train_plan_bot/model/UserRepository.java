package ru.stradiavanti.train_plan_bot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

// для взаимодейтсвия с таблицей (б/д).
// Параметры шаблона : 1. Класс, который описывает таблицу, 2. Long (для идентфикации
// пользователя будем использовать его id)
// Primary Key,
// тип столбца,
// который идентифицирует каждую из записей таблицы.
// Механизм Spring, который предоставляет методы для работы с базой данных
// В качестве параметров шаблона передаем 1. тип сущности 2. тип первичного ключа
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
