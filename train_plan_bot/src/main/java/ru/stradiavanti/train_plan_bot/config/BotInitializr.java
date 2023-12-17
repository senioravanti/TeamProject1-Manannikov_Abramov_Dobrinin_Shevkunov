package ru.stradiavanti.train_plan_bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.stradiavanti.train_plan_bot.service.TelegramBot;

@Component
@Slf4j
public class BotInitializr {

  @Autowired
  // Создаст экземпляр класса TelegramBot при создании экземпляра BotInitializr.
  private TelegramBot bot;

  @EventListener({ContextRefreshedEvent.class})
  public void init () throws TelegramApiException {
    // Это обязательно
    TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

    try {
      // Запускаем бота
      // Благодаря Spring мы не создаем явно экземпляр класса TelegramBot, не пишем
      // такой код : telegramBotsApi.registerBot(new bot());
      telegramBotsApi.registerBot(bot);
    } catch (TelegramApiException e) {
      log.error("Error occurred : " + e.getMessage());
    }

  }
}
