package ru.stradiavanti.train_plan_bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.stradiavanti.train_plan_bot.config.BotConfig;
import ru.stradiavanti.train_plan_bot.model.Trainer;
import ru.stradiavanti.train_plan_bot.model.TrainerRepository;
import ru.stradiavanti.train_plan_bot.model.User;
import ru.stradiavanti.train_plan_bot.model.UserRepository;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Component
@Slf4j
// TelegramLongPollingBot - бот периодически обращается к серверу за информацией об
// отправленных сообщениях.
// TelegramWebHookBot - бот получает информацию об отправленных сообщениях в момент их
// отправки. Более эффективен, но сложнее в реализации.

/* TelegramLongPollingBot :: Абстрактный класс, содержит интерфейсы - виртуальные
методы, которые нужно реализовать */
public class TelegramBot extends TelegramLongPollingBot {
  /* Свойства */
  // Объявляем экземпляр класса BotConfig
  @Autowired
  // Включаем зависимость и внедряем ее
  private UserRepository userRepository;
  @Autowired
  private TrainerRepository trainerRepository;
  private final BotConfig config;

  private final String CALLBACK_YES = "__YES";
  private final String CALLBACK_NO = "__NO";
  private final String CALLBACK_SET_SUBSCRIPTION = "__SET_SUBSCRIPTION";
  private final String CALLBACK_DELETE_SUBSCRIPTION = "__DELETE_SUBSCRIPTION";
  private final String CALLBACK_FITNESS = "__FITNESS";
  private final String CALLBACK_BODYBUILDING = "__BODYBUILDING";
  private final String CALLBACK_YOGA = "__YOGA";

  private final String HELP_TEXT = "\n***\n\nМои ф-ции :\n" +
    "- Составить расписание /getschedule" +
    "тренировок\n- Записать к тренеру : /setcoach\n- Отсылать тебе оповещения пока ты не" +
    " начнешь регулярно" +
    "ходить на" +
    " трени (или не добавишь меня в черный список).\n\n***";

  private Long trainerId;

  /* Методы */
  public TelegramBot(BotConfig config) {
    super(config.getToken());
    this.config = config;

    // Реализуем меню
    List<BotCommand> listofCommands = new ArrayList<BotCommand>();

    listofCommands.add(new BotCommand("/start", "👉 Начать диалог"));
    listofCommands.add(new BotCommand("/register", "💸 Записаться в зал."));
    listofCommands.add(new BotCommand("/get_subscriptions", "🤑️ Просмотреть доступные " +
      "мне" +
      "абонементы"));
    listofCommands.add(new BotCommand("/getschedule", "🗓️ Выведи расписание"));
    listofCommands.add(new BotCommand("/setcoach", "🦾 Запиши к тренеру"));

    listofCommands.add(new BotCommand("/help", "🆘 Поясни за функционал"));
    listofCommands.add(new BotCommand("/settings", "⚙️ Настрой свое расписание"));

    try {

      this.execute(
        new SetMyCommands(
          listofCommands, new BotCommandScopeDefault(), null
        )
      );

    } catch (TelegramApiException e) {

      log.error("Error setting bot's command list: " + e.getMessage());

    }

  }

  @Override
  public String getBotUsername() {
    return config.getBotName();
  }

  @Override
  // Обработчик события, основной метод приложения, через который осуществляем
  // Пользователь отправил сообщение, мы должны на него ответить.
  public void onUpdateReceived(Update update) {


    if (update.hasMessage() && update.getMessage().hasText()) {

      String mesText = update.getMessage().getText();
      String name = update.getMessage().getChat().getFirstName();
      // Бот должен знать идентификатор пользователя
      Long chatId = update.getMessage().getChatId();

      switch (mesText) {
        case "/start":
          //registerUser(update.getMessage());
          startCommandReceived(chatId, name);

          break;
        case "/register":
          // Выносим все в отдельные методы
          register(chatId);
          break;
        case "/help":
          // Выводим список команд.
          justSendMessage(chatId, HELP_TEXT);
          log.info("Replied to user " + name);

          break;
        default:
          justSendMessage(chatId, "😬 Я не знаю что делать, такая команда в меня не " +
          "заложена");
      }
    // Вместо текста нажали на кнопку.
    } else if (update.hasCallbackQuery()) {
      // Не сообщение, а отдельный объект, представляет событие, нажали на кнопку,
      // если кнопка была прикреплена к сообщению, отпр. ботом то будет присутствовать
      // поле message.
      String callbackData = update.getCallbackQuery().getData();
      long mesId = update.getCallbackQuery().getMessage().getMessageId();
      long chatId = update.getCallbackQuery().getMessage().getChatId();

      String text;

      if (callbackData.equals(CALLBACK_YES)) {

        DateFormat dt = new SimpleDateFormat("dd.MM.yy");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 3);

        registerUser(update.getCallbackQuery().getMessage());

        text = "🎉 Поздравляю вы записаны в наш клуб, вам выдан бесплатный " +
          "абонемент на 3 месяца до " + dt.format(cal.getTime());
        callbackDataEditMes(chatId, mesId, text);
        sendTrainer(chatId, trainerId);

      } else if (callbackData.equals(CALLBACK_SET_SUBSCRIPTION)) {
        // Выбираем направление тренировок
        chooseUserSpecialization(chatId);

      } else if (callbackData.equals(CALLBACK_NO)) {
        text = "🤑 Пожалуйста подумайте еще раз, очень хотим вас видеть в нашем клубе.";
        callbackDataEditMes(chatId, mesId, text);

      } else if (callbackData.equals(CALLBACK_DELETE_SUBSCRIPTION)) {
        text = "До свидания, надеюсь мы еще увидимся снова";
        callbackDataEditMes(chatId, mesId, text);
        userRepository.deleteById(chatId);

      } else if (callbackData.equals(CALLBACK_FITNESS)) {
        trainerId = getSpecialTrainerId("Фитнес");
        register(chatId);

      } else if (callbackData.equals(CALLBACK_BODYBUILDING)) {
        trainerId = getSpecialTrainerId("Бодибилдинг");
        register(chatId);

      } else if (callbackData.equals(CALLBACK_YOGA)) {
        trainerId = getSpecialTrainerId("Йога");
        register(chatId);

      }

    }

  }
  private void sendTrainer(Long chatId, Long id) {
    Trainer trainer = trainerRepository.findById(id).get();
    SendPhoto sendPhoto = new SendPhoto();
    sendPhoto.setChatId(String.valueOf(chatId));
    sendPhoto.setPhoto(new InputFile(trainer.getPhotoPath()));
    sendPhoto.setCaption("Ваш тренер: " + trainer.getFirstName() + " " + trainer.getLastName());
    try {
      execute(sendPhoto);
    } catch (TelegramApiException e) {
      log.error("Error occurred : " + e.getMessage());
    }
  }
  private void justSendMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(text);
    sendMessage(message);

  }
  private void sendMessage(SendMessage message) {
    try {
      // Метод execute может вызвать исключение, но не обрабатывает его => его нужно
      // обработать
      execute(message);
    } catch (TelegramApiException e) {
      // Исключение вызывается если ссылка на метод - null.
      log.error("Error occurred : " + e.getMessage());
    }
  }

  private void startCommandReceived(long chatId, String name) {
    String answer;

    SendMessage message = new SendMessage();
    message.setChatId(chatId);

    if (userRepository.findById(chatId).isEmpty()) {
      answer = "🤔 Новые лица в нашем заведении, что-то я тебя не узнаю, давай я " +
        "оформлю тебе абонемент в наш фитнес-клуб, сделаю из тебя гигачада !";

      // Вынести в отдельный метод

      InlineKeyboardMarkup menu = new InlineKeyboardMarkup();
      List< List<InlineKeyboardButton> > buttonsMatrix =
        new ArrayList< List<InlineKeyboardButton> >();
      List< InlineKeyboardButton > buttonsRow = new ArrayList< InlineKeyboardButton >();

      var butSetSubscription = new InlineKeyboardButton();
      butSetSubscription.setText("🦾 Оформить абонемент");
      butSetSubscription.setCallbackData(CALLBACK_SET_SUBSCRIPTION);

      var butNo = new InlineKeyboardButton();
      butNo.setText("🤬 Не, я не хочу заниматься");
      butNo.setCallbackData(CALLBACK_NO);

      buttonsRow.add(butNo);
      buttonsRow.add(butSetSubscription);

      buttonsMatrix.add(buttonsRow);
      menu.setKeyboard(buttonsMatrix);
      message.setReplyMarkup(menu);

    } else {
      answer = "🫡 Здравия желаю " + name + " !\n\nЯ тебя узнал, следовательно " +
        "помогу" +
        " " +
        "тебе составить новое " +
        "расписание тренировок, или выведу действующее.";

      InlineKeyboardMarkup menu = new InlineKeyboardMarkup();
      List< List<InlineKeyboardButton> > buttonsMatrix =
              new ArrayList< List<InlineKeyboardButton> >();
      List< InlineKeyboardButton > buttonsRow = new ArrayList< InlineKeyboardButton >();

      var butDeleteSubscription = new InlineKeyboardButton();
      butDeleteSubscription.setText("Перестать ходить на тренировки");
      butDeleteSubscription.setCallbackData(CALLBACK_DELETE_SUBSCRIPTION);

      buttonsRow.add(butDeleteSubscription);
      buttonsMatrix.add(buttonsRow);
      menu.setKeyboard(buttonsMatrix);
      message.setReplyMarkup(menu);
    }

    message.setText(answer);
    sendMessage(message);

    log.info("Replied to user " + name);
  }

  private void register(long chatId) {
    // TODO проверять есть ли пользователь в базе данных, если нет то регистрируем.
    SendMessage message = new SendMessage();
    message.setChatId(chatId);

    if (userRepository.findById(chatId).isEmpty()) {

      InlineKeyboardMarkup menu = new InlineKeyboardMarkup();
      List< List<InlineKeyboardButton> > buttonsMatrix = new ArrayList< List<InlineKeyboardButton> >();
      List< InlineKeyboardButton > buttonsRow = new ArrayList< InlineKeyboardButton >();

      message.setText("🫵 В нашем фитнес клубе для новых посетителей 3 месяца " +
        "бесплатно, потом " +
        "10 000 руб " +
        "в " +
        "месяц," +
        " " +
        "вы согласны записаться ?");

      var but_yes = new InlineKeyboardButton();
      but_yes.setText("Да");
      but_yes.setCallbackData(CALLBACK_YES);

      var but_no = new InlineKeyboardButton();
      but_no.setText("Нет");
      but_no.setCallbackData(CALLBACK_NO);

      buttonsRow.add(but_no);
      buttonsRow.add(but_yes);

      buttonsMatrix.add(buttonsRow);
      menu.setKeyboard(buttonsMatrix);

      message.setReplyMarkup(menu);
    } else {
      // Узнать как извлекать данные из базы данных (наверное с помощью аннотации Query)
      message.setText("💖 Вы уже оформили абонемент и он пока действует");
    }
    sendMessage(message);
  }


  private void registerUser(Message message) {
    var chatId = message.getChatId();
    var chat = message.getChat();


    User user = new User();
    user.setChatId(chatId);
    user.setLastName(chat.getLastName());
    user.setFirstName(chat.getFirstName());
    user.setTrainerId(trainerId);
    user.setStartSubscriptionDate(LocalDate.now());
    user.setEndSubscriptionDate(user.getStartSubscriptionDate().plusMonths(3));
    // Последний параметр надо из таблицы брать.
    userRepository.save(user);
    // Оператор + -> перегруженный метод toString
    log.info("User saved" + user);
  }

  private void chooseUserSpecialization(Long chatId) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);

    InlineKeyboardMarkup menu = new InlineKeyboardMarkup();
    List< List<InlineKeyboardButton> > buttonsMatrix = new ArrayList< List<InlineKeyboardButton> >();
    List< InlineKeyboardButton > buttonsRow = new ArrayList< InlineKeyboardButton >();

    message.setText("💪🏼 Чем бы вы хотели заниматься в нашем зале ?");

    var but_fitness = new InlineKeyboardButton();
    but_fitness.setText("🤸🏽 Фитнес");
    but_fitness.setCallbackData(CALLBACK_FITNESS);

    var but_bodybuilding = new InlineKeyboardButton();
    but_bodybuilding.setText("🏋️‍♂️ Бодибилдинг");
    but_bodybuilding.setCallbackData(CALLBACK_BODYBUILDING);

    var but_yoga = new InlineKeyboardButton();
    but_yoga.setText("️🧘🏽‍♀️ Йога");
    but_yoga.setCallbackData(CALLBACK_YOGA);

    buttonsRow.add(but_fitness);
    buttonsRow.add(but_bodybuilding);
    buttonsRow.add(but_yoga);

    buttonsMatrix.add(buttonsRow);
    menu.setKeyboard(buttonsMatrix);

    message.setReplyMarkup(menu);

    sendMessage(message);
  }
  private Long getSpecialTrainerId(String specialization) {
    List<Long> fitnessTrainersId = new ArrayList<Long>();
    for (Trainer t : trainerRepository.findAll()) {
      if (t.getSpecialization().equals(specialization)) {
        fitnessTrainersId.add(t.getTrainerId());
      }
    }
    int randomIndex = (int) (Math.random() * fitnessTrainersId.size());
    return fitnessTrainersId.get(randomIndex);
  }

  private void callbackDataEditMes(long chatId, long mesId, String text) {
    EditMessageText mes = new EditMessageText();
    mes.setChatId(chatId);
    mes.setMessageId((int) mesId);
    mes.setText(text);
    try {
      // Отправим сообщение
      execute(mes);
    } catch (TelegramApiException e) {
      log.error("Error occurred : " + e.getMessage());
    }
  }

}
