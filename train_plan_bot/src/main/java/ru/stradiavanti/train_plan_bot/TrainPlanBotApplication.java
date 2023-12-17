package ru.stradiavanti.train_plan_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Аннотация будет загружать приложение.
@SpringBootApplication
public class TrainPlanBotApplication {

    public static void main(String[] args) {
        // Главный Класс, запускает Spring приложение
        SpringApplication.run(TrainPlanBotApplication.class, args);
    }
}
