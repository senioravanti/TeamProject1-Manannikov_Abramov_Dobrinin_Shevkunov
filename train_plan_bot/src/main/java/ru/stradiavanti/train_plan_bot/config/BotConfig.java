package ru.stradiavanti.train_plan_bot.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("application.properties")

public class BotConfig {
  @Value("${bot.name}")
  private String botName;
  @Value("${bot.key}")
  private String token;

}
