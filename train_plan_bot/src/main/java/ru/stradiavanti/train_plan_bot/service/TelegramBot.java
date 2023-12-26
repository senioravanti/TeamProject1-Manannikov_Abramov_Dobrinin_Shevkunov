
package ru.stradiavanti.train_plan_bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.stradiavanti.train_plan_bot.config.BotConfig;
import ru.stradiavanti.train_plan_bot.model.Trainer;
import ru.stradiavanti.train_plan_bot.model.TrainerRepository;
import ru.stradiavanti.train_plan_bot.model.User;
import ru.stradiavanti.train_plan_bot.model.UserRepository;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
  public enum UserStatus {
    Unknown, PotentialClient, Client
  }
  private final BotConfig config;
  /* Связанные с базой данных свойства  */
  @Autowired
  // Включаем зависимость и внедряем ее
  private UserRepository userRepository;
  @Autowired
  private TrainerRepository trainerRepository;
  private UserStatus userStatus;
  User curUser;
  /* Часто используемые свойства, связанные с пользователем */
  private Long chatId;
  private Integer mesId;
  
  private String curUserName;
  private List<BotCommand> listOfCommands = null;
  /* События */
  private final String __APPLY_SUBSCRIPTION_BUTTON = "__APPLY_SUBSCRIPTION_BUTTON";
  private final String __REJECT_SUBSCRIPTION_BUTTON = "__REJECT_SUBSCRIPTION_BUTTON";
  private final String __SELECT_COACH_BUTTON = "__SELECT_SELECT_COACH_BUTTON";
  private final String __REJECT_COACH_BUTTON = "__REJECT_COACH_BUTTON";
  private final String __COACHES_LIST_BUTTON = "__COACHES_LIST_BUTTON";
  private final String __REGISTER_CLIENT_BUTTON = "__REGISTER_CLIENT_BUTTON";
  private final String __COACH_INFO_BUTTON = "__COACH_INFO_BUTTON";
  private final String __RATE_COACH_WORK_BUTTON = "__RATE_COACH_WORK_BUTTON";
  private final String __CHANGE_COACH_BUTTON = "__CHANGE_COACH_BUTTON";
  private final String __SET_COACH_RATING_ONE_STAR = "__SET_COACH_RATING_ONE_STAR";
  private final String __SET_COACH_RATING_TWO_STAR = "__SET_COACH_RATING_TWO_STAR";
  private final String __SET_COACH_RATING_THREE_STAR = "__SET_COACH_RATING_THREE_STAR";
  private final String __SET_COACH_RATING_FOUR_STAR = "__SET_COACH_RATING_FOUR_STAR";
  private final String __SET_COACH_RATING_FIVE_STAR = "__SET_COACH_RATING_FIVE_STAR";
  private final String __SET_SCHEDULE = "__SET_SCHEDULE";
  private final String __GET_SCHEDULE = "__GET_SCHEDULE";

  // Флаги
  boolean choosingCoach;
  boolean changesCoachRating;
  boolean entersCommand;
  boolean settedSchedule;
  // ---
  private String helpText;



  public TelegramBot(BotConfig config) {
    super(config.getToken());
    this.config = config;
    choosingCoach = false;
    changesCoachRating = false;
    entersCommand = true;
    settedSchedule = false;

    listOfCommands = new ArrayList<>();

    curUser = null;
    userStatus = UserStatus.Unknown;
    // Реализуем меню
    makeMenu();

  }

  @Override
  public String getBotUsername() {
    return config.getBotName();
  }

  @Override
  // Обработчик события, основной метод приложения, через который осуществляем
  // Пользователь отправил сообщение, мы должны на него ответить.
  public void onUpdateReceived(Update update) {

    // TODO Переделать меню, добавить нормальные пункты : тренер и расписание, где
    //  можно выбрать тренера, получить всю информацию, настроить расписание и т.п.
    if (update.hasMessage() && update.getMessage().hasText()) {

      String userMessageText = update.getMessage().getText();
      curUserName = update.getMessage().getChat().getFirstName();
      // Бот должен знать идентификатор пользователя
      chatId = update.getMessage().getChatId();

      if (entersCommand) {
        on_entersCommand(userMessageText);
      }
      else if (choosingCoach) {
        // Пользователь ввел ФИО тренера
        List<String> name = List.of(userMessageText.split(" "));
        SendMessage botMessage = new SendMessage();
        botMessage.setChatId(chatId);
        botMessage.setReplyToMessageId(update.getMessage().getMessageId());

        String botMessageText;
        if (name.size() != 3) {
          botMessageText = "😭 Вы ввели некорректное ФИО !";
        } else {
          Optional<Trainer> trainer =
            trainerRepository.findByLastNameAndFirstNameAndFatherName(
            name.get(0),
            name.get(1),
            name.get(2)
          );
          if (trainer.isPresent()) {
            curUser = userRepository.findById(chatId).get();
            curUser.setTrainerId(trainer.get().getId());

            botMessageText =
              "🎉 Поздравляю " + name.get(0) + " " + name.get(1) + " " + name.get(2) +
              " - " +
              "ваш новый тренер.";
            trainer.get().setIsFree(false);

            trainerRepository.save(trainer.get());
            userRepository.save(curUser);

          } else {
            botMessageText = "🤔 Хм, в нашей базе данных нет тренера с таким ФИО.";
          }
        }
        botMessage.setText(botMessageText);
        sendMessage(botMessage);
        choosingCoach = false;
        changesCoachRating = false;
        entersCommand = true;
      }
      else if (changesCoachRating) {
        try {
          Double newRating = Double.parseDouble(userMessageText);
          on_updateCoachRatingButton("✍️ Обновляю рейтинг тренера ...", newRating);
        } catch (NumberFormatException e) {
          justSendMessage(
            chatId,
            "😬 Вы ввели строку вместо числа !",
            Optional.empty()
          );
        }
        choosingCoach = false;
        changesCoachRating = false;
        entersCommand = true;
      }
    // Нажали на кнопку.
    }
    else if (update.hasCallbackQuery()) {
      CallbackQuery callbackQuery = update.getCallbackQuery();

      Message sentMessage = callbackQuery.getMessage();
      String data = callbackQuery.getData();

      mesId = sentMessage.getMessageId();
      chatId = sentMessage.getChatId();

      curUserName = update.getCallbackQuery().getMessage().getChat().getFirstName();

      String editedMessageText;

      if (data.equals(__APPLY_SUBSCRIPTION_BUTTON)) {

        DateFormat dt = new SimpleDateFormat("dd.MM.yy");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 3);

        registerUser(sentMessage);

        editedMessageText = "🎉 Поздравляю вы записаны в наш фитнес клуб, вам выдан бесплатный " +
          "абонемент на 3 месяца до " + dt.format(cal.getTime());

        userStatus = UserStatus.Client;
        makeMenu();

        sendEditedMessage(chatId, mesId, editedMessageText, Optional.empty());

      }
      else if (data.equals(__REGISTER_CLIENT_BUTTON)) {
        on_registerCommand();

      }
      else if (data.equals(__REJECT_SUBSCRIPTION_BUTTON)) {
        editedMessageText = "🤑 Пожалуйста подумайте еще раз, очень хотим вас видеть в нашем клубе.";
        sendEditedMessage(chatId, mesId, editedMessageText, Optional.empty());

      }
      else if (data.equals(__COACHES_LIST_BUTTON)) {
        editedMessageText = getCoachesList();

        sendEditedMessage(
          chatId,
          mesId,
          editedMessageText,
          Optional.of(
            makeInlineKeyboardMarkup(
              List.of(
                "Определиться с тренером",
                "Нет подходящего тренера !"
              ),
              List.of(
                __SELECT_COACH_BUTTON,
                __REJECT_COACH_BUTTON
              ),
              2
            )
          )
        );
      }
      else if (data.equals(__SELECT_COACH_BUTTON)) {

        // Будем ждать ввод пользователя.
        justSendMessage(
          chatId,
          "✍️ Введи ФИО тренера, из списка выше, между " +
          "словами " +
          "ставь пробелы, не пиши лишнего !",
          Optional.empty()
        );
        choosingCoach = true;
        changesCoachRating = false;
        entersCommand = false;


      }
      else if (data.equals(__REJECT_COACH_BUTTON)) {
        editedMessageText = "😬 Очень жаль, специально для тебя мы обязательно наймем " +
          "Арнольда Шварцнегера.";

        sendEditedMessage(
          chatId,
          mesId,
          editedMessageText,
          Optional.empty()
        );
      }
      else if (data.equals(__CHANGE_COACH_BUTTON)) {
        User client = userRepository.findById(chatId).get();
        Trainer trainer = trainerRepository.findById(client.getTrainerId()).get();
        client.setTrainerId(-1L);
        trainer.setIsFree(true);

        userRepository.save(client);
        trainerRepository.save(trainer);

        editedMessageText = "😎 Такс, теперь ты гоняешь без тренера, но в любой момент " +
          "можешь" +
          " " +
          "выбрать другого.";

        sendEditedMessage(
          chatId,
          mesId,
          editedMessageText,
          Optional.of(
            makeInlineKeyboardMarkup(
              List.of("Выбрать тренера"),
              List.of(__COACHES_LIST_BUTTON),
              1
            )
          )
        );

      }
      else if (data.equals(__COACH_INFO_BUTTON)) {
        User client = userRepository.findById(chatId).get();
        Trainer trainer = trainerRepository.findById(client.getTrainerId()).get();

        String trainerInfo =
          "\nИнформация о твоем тренере\n\n- ФИО : " + trainer.getLastName() + " " + trainer.getFirstName() + " " + trainer.getFatherName() +
          "\n- Специализация : " + trainer.getSpecialization() +
          String.format("\n- Рейтинг : %.2f", trainer.getRating()
          );

        SendPhoto botPhotoMessage = new SendPhoto();
        botPhotoMessage.setChatId(chatId);
        botPhotoMessage.setPhoto(new InputFile(trainer.getImageUrl()));
        botPhotoMessage.setCaption(trainerInfo);

        try {
          execute(botPhotoMessage);
        } catch (TelegramApiException e) {
          log.error("Не удалось отправить информацию о тренере :\n" + e.getMessage());
        }

      }
      else if (data.equals(__RATE_COACH_WORK_BUTTON)) {
        editedMessageText = "🤩 Сколько звезд ты поставишь своему тренеру ?\nНажми на " +
          "кнопку или напиши дробное число звезд";

        changesCoachRating = true;
        choosingCoach = false;
        entersCommand = false;

        sendEditedMessage(
          chatId,
          mesId,
          editedMessageText,
          Optional.of(
            makeInlineKeyboardMarkup(
              List.of(
                "⭐", "⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐⭐⭐"
              ),
              List.of(
                __SET_COACH_RATING_ONE_STAR,
                __SET_COACH_RATING_TWO_STAR,
                __SET_COACH_RATING_THREE_STAR,
                __SET_COACH_RATING_FOUR_STAR,
                __SET_COACH_RATING_FIVE_STAR
              ),
              5
            )
          )
        );

      }
      else if (data.equals(__SET_COACH_RATING_ONE_STAR)){
        on_updateCoachRatingButton("🤘 Мы его уволим, не волнуйтесь.",
          1.0);
        changesCoachRating = false;
      }
      else if (data.equals(__SET_COACH_RATING_TWO_STAR)){
        on_updateCoachRatingButton("✍️ Зафиксировал, лично сделаю тренеру последнее китайское...",
          2.0);
        changesCoachRating = false;

      }
      else if (data.equals(__SET_COACH_RATING_THREE_STAR)){
        on_updateCoachRatingButton("😬 Понял, скажу начальнику конторы чтобы поговорил с ним.",
          3.0);
        changesCoachRating = false;

      }
      else if (data.equals(__SET_COACH_RATING_FOUR_STAR)){
        on_updateCoachRatingButton("🎉 Рад что вы оценили нашего тренера !", 4.0);
        changesCoachRating = false;

      }
      else if (data.equals(__SET_COACH_RATING_FIVE_STAR)){
        on_updateCoachRatingButton("🤩 Выдам вашему тренеру годовую зарплату в качестве премии !",
          5.0);
        changesCoachRating = false;

      }
      else if (data.equals(__SET_SCHEDULE)){
        SendPoll sendPoll = new SendPoll();
        sendPoll.setChatId(chatId);

        sendPoll.setIsAnonymous(false);
        sendPoll.setAllowMultipleAnswers(true);
        sendPoll.setQuestion("Выберите дни недели, в которые желаете ходить на тренировки");

        List<String> options = new ArrayList<>();
        options.add("Понедельник");
        options.add("Вторник");
        options.add("Среда");
        options.add("Четверг");
        options.add("Пятница");

        sendPoll.setOptions(options);

        try {
          execute(sendPoll).getPoll();
        }
        catch (TelegramApiException e) {
          log.error("Ошибка при отправке опроса\n" + e.getMessage());
        }

      }
      else if (data.equals(__GET_SCHEDULE)) {
        if (settedSchedule) {
          editedMessageText = "✍️ Ваше текущее расписание тренировок :\n\n";

          curUser = userRepository.findById(chatId).get();
          LocalDate currentDate = LocalDate.now();
          DayOfWeek currentDayOfWeek = currentDate.getDayOfWeek();

          LocalDate nearestDate;
          int daysUntill;

          if (curUser.getMonday()) {
            daysUntill = DayOfWeek.MONDAY.getValue() - currentDayOfWeek.getValue();
            if (daysUntill <= 0) daysUntill += 7;
            editedMessageText =
              editedMessageText.concat("1. Понедельник " + currentDate.plusDays(daysUntill).toString() +
                "\n");
          }
          if (curUser.getTuesday()) {
            daysUntill = DayOfWeek.TUESDAY.getValue() - currentDayOfWeek.getValue();
            if (daysUntill <= 0) daysUntill += 7;
            editedMessageText =
              editedMessageText.concat("2. Вторник " + currentDate.plusDays(daysUntill).toString() +
                "\n");
          }
          if (curUser.getWednesday()) {
            daysUntill = DayOfWeek.WEDNESDAY.getValue() - currentDayOfWeek.getValue();
            if (daysUntill <= 0) daysUntill += 7;
            editedMessageText =
              editedMessageText.concat("3. Среда " + currentDate.plusDays(daysUntill).toString() +
                "\n");
          }
          if (curUser.getThursday()) {
            daysUntill = DayOfWeek.THURSDAY.getValue() - currentDayOfWeek.getValue();
            if (daysUntill <= 0) daysUntill += 7;
            editedMessageText =
              editedMessageText.concat("4. Четверг " + currentDate.plusDays(daysUntill).toString() +
                "\n");
          }
          if (curUser.getFriday()) {
            daysUntill = DayOfWeek.FRIDAY.getValue() - currentDayOfWeek.getValue();
            if (daysUntill <= 0) daysUntill += 7;
            editedMessageText =
              editedMessageText.concat("5. Пятница " + currentDate.plusDays(daysUntill).toString() +
                "\n");
          }
        } else {
          editedMessageText = "😬 Ваше расписание тренировок еще не сформировано, " +
            "рекомендуем вам сформировать его как можно скорее, для этого " +
            "воспользуйтесь командой /schedule";
        }

        sendEditedMessage(
          chatId,
          mesId,
          editedMessageText,
          Optional.empty()
        );

      }
    }
    else if (update.hasPollAnswer()) {
      chatId = update.getPollAnswer().getUser().getId();

      User client = userRepository.findById(chatId).get();

      for (var it : update.getPollAnswer().getOptionIds()) {
        switch (it) {
          case 0 :
            client.setMonday(true);
            break;
          case 1:
            client.setTuesday(true);
            break;
          case 2:
            client.setWednesday(true);
            break;
          case 3:
            client.setThursday(true);
            break;
          case 4:
            client.setFriday(true);
            break;
        }
      }

      userRepository.save(client);

      if (!settedSchedule) settedSchedule = true;

      justSendMessage(chatId,
        "🎉 Новое расписание успешно сформировано",
        Optional.empty()
        );
    }
  }

  /* Служебные методы */
  private InlineKeyboardMarkup makeInlineKeyboardMarkup(
    List<String> buttonsText,
    List<String> buttonsCallBack,
    int count
  )
  {
    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

    List< List< InlineKeyboardButton > > buttonsMatrix = new ArrayList<>();
    List< InlineKeyboardButton > buttonsRow = new ArrayList<>();
    InlineKeyboardButton button;

    for (int i = 0; i < count; ++i) {
      if (buttonsRow.size() >= 3) {
        buttonsMatrix.add(buttonsRow);
        buttonsRow = new ArrayList<>();
      }
      button = new InlineKeyboardButton();
      button.setText(buttonsText.get(i));
      button.setCallbackData(buttonsCallBack.get(i));
      buttonsRow.add(button);
    }
    buttonsMatrix.add(buttonsRow);

    keyboard.setKeyboard(buttonsMatrix);
    return keyboard;
  }

  private void on_entersCommand(String userMessageText) {
    if (userMessageText.equals("/start")) {
      on_startCommandReceived();
    }
    else if (userMessageText.equals("/register") && userStatus.equals(UserStatus.PotentialClient)) {
      on_registerCommand();
    }
    else if (userMessageText.equals("/coach") && userStatus.equals(UserStatus.Client)) {
      on_coachCommand();
    }
    else if (userMessageText.equals("/schedule") && userStatus.equals(UserStatus.Client)) {
      on_scheduleCommand();
    }
    else if (userMessageText.equals("/help")) {
      justSendMessage(
        chatId,
        helpText,
        Optional.empty()
      );
      log.info("Ответил пользователю " + curUserName + " на команду /help.");
    }
    else {
      justSendMessage(
        chatId,
        "😬 В меню такой команды НЕТ.",
        Optional.empty()
      );
    }
  }
  private void on_startCommandReceived() {

    String answer;

    SendMessage message = new SendMessage();
    message.setChatId(chatId);

    if (userRepository.findById(chatId).isEmpty()) {
      answer = "🤔 Новые лица в нашем заведении, что-то я тебя не узнаю, давай я " +
        "оформлю тебе абонемент в наш фитнес-клуб, сделаю из тебя гигачада !";

      message.setReplyMarkup(
        makeInlineKeyboardMarkup(
          List.of(
            "🦾 Оформи абонемент",
            "🙃 Я пока не готов"
          ),
          List.of(
            __REGISTER_CLIENT_BUTTON,
            __REJECT_SUBSCRIPTION_BUTTON
          ),
          2
        )
      );
      userStatus = UserStatus.PotentialClient;
      makeMenu();
    }
    else {
      answer = "🫡 Здравия желаю " + curUserName + " !\n\nЯ тебя узнал, следовательно " +
        "помогу" +
        " " +
        "тебе составить новое " +
        "расписание тренировок, или выведу действующее.";
      userStatus = UserStatus.Client;
      makeMenu();
    }

    message.setText(answer);
    sendMessage(message);

    log.info("Ответил пользователю " + curUserName + " на команду /start.");
  }
  private void registerUser(Message message) {
    var chatId = message.getChatId();
    var chat = message.getChat();

    User user = new User(
      chatId, -1L, 0.0, chat.getFirstName(), chat.getLastName(),
      LocalDate.now(), LocalDate.now().plusMonths(3),
      0.0,
      false, false, false, false, false

    );

    // Последний параметр надо из таблицы брать.
    userRepository.save(user);
    userStatus = UserStatus.Client;
    makeMenu();
    // Оператор + -> перегруженный метод toString
    log.info("User saved" + user);
  }

  private void on_registerCommand() {
    // TODO проверять есть ли пользователь в базе данных, если нет то регистрируем.
    SendMessage message = new SendMessage();
    message.setChatId(chatId);

    if (userRepository.findById(chatId).isEmpty()) {
      message.setText("🫵 В нашем фитнес клубе для новых посетителей 3 месяца " +
        "бесплатно, потом " +
        "10_000 руб " +
        "в " +
        "месяц," +
        " " +
        "вы согласны записаться ?");

      message.setReplyMarkup(
        makeInlineKeyboardMarkup(
          List.of("Да я согласен !", "Нет, я пока думаю"),
          List.of(__APPLY_SUBSCRIPTION_BUTTON, __REJECT_SUBSCRIPTION_BUTTON),
          2
        )
      );

    }
    else {
      // Узнать как извлекать данные из базы данных (наверное с помощью аннотации Query)
      message.setText("💖 Вы уже оформили абонемент и он пока действует");
    }

    sendMessage(message);
    log.info("Ответил пользователю " + curUserName + "на команду /register.");
  }

  /* Все что связанно с тренером */
  private Double updateCoachRatingInTable() {
    // Эту команду можно выполнить только при наличии тренера.
    Trainer trainer =
      trainerRepository.findById(curUser.getTrainerId()).get();

    List<User> users = userRepository.findAll();

    Double sumOfVotes = 0.0;
    Integer numOfVotes = 0;
    Double rating;

    for (var it : users) {
      if (Objects.equals(it.getTrainerId(), trainer.getId())) {
        sumOfVotes += it.getTrainerRaiting(); numOfVotes++;
      }
    }

    if (numOfVotes == 0) {
      rating = 0.0;
    }
    else {
      rating = sumOfVotes/numOfVotes;
    }

    trainer.setRating(rating);
    trainerRepository.save(trainer);

    return rating;
  }
  private void on_updateCoachRatingButton(String messageText, Double newRating) {

    curUser = userRepository.findById(chatId).get();
    curUser.setTrainerRaiting(newRating);
    userRepository.save(curUser);

    justSendMessage(
      chatId,
      messageText + "\nОбновленный рейтинг : " + updateCoachRatingInTable().toString(),
      Optional.empty()
    );

  }
  private void on_coachCommand() {
    SendMessage botMessage = new SendMessage();
    botMessage.setChatId(chatId);

    if
    (
        userRepository.findById(chatId).get().getTrainerId() == -1
    )
    {
      botMessage.setReplyMarkup(
        makeInlineKeyboardMarkup(
          List.of(
            "Выбрать тренера"
          ),
          List.of(
            __COACHES_LIST_BUTTON
          ),
          1
        )
      );
      botMessage.setText("🧙‍♂️ На данный момент у вас не тренера, вы занимаетесь " +
        "самостоятельно, рекомендую вам выбрать тренера с самым высоким рейтингом !");
    }
    else {
      botMessage.setReplyMarkup(
        makeInlineKeyboardMarkup(
          List.of(
            "Справка о моем тренере",
            "Оценить тренера",
            "Сменить тренера"
          ),
          List.of(
            __COACH_INFO_BUTTON,
            __RATE_COACH_WORK_BUTTON,
            __CHANGE_COACH_BUTTON
          ),
          3
        )
      );

      botMessage.setText("️🤔 У тебя уже есть крутейший наставник, чего же тебе еще надо ?");
      log.info("Ответил пользователю " + curUserName + " на команду /coach.");
    }
    sendMessage(botMessage);
  }
  // TODO Настроить расписание
  private void on_scheduleCommand() {
    justSendMessage(
      chatId,
      "Что вы желаете сделать ?",
      Optional.of(

        makeInlineKeyboardMarkup(
          List.of(
            "🗓️ Получить текущее расписание",
            "🧱 Сформировать новое"
          ),
          List.of(
            __GET_SCHEDULE,
            __SET_SCHEDULE
          ),
          2
        )

      )
    );

  }

  // TODO Реализовать вывод списка тренеров, считывать выбор пользователя, закрепить
  //  за ним тренера с опр. идентификатором
  private String getCoachesList() {
    String coachesList = "Наши тренеры :\n\nТренеры к которым можно записаться отмечены " +
      "символом 🟩, занятые отмечены 🟥" +
      "\n\n";
    List<Trainer> trainers = trainerRepository.findAll();
    Integer i = 0;
    for (var it : trainers) {
      coachesList = coachesList.concat(i.toString() + ". ");
      if (
        it.getFatherName().endsWith("вна")
      )
      {
        coachesList = coachesList.concat(" 🧘‍♀️ ");
      } else {
        coachesList = coachesList.concat(" 🤺 ");
      }
      coachesList = coachesList.concat(it.getLastName() + " " + it.getFirstName() + " " + it.getFatherName() + " 🤩 " +
         String.format("%.2f",
           it.getRating()
         ));

      if (it.getIsFree()) coachesList = coachesList.concat(" 🟩\n\n");
      else coachesList = coachesList.concat(" 🟥\n\n");
      i++;
    }
    return coachesList;

  }

  /* Все что связано с меню пользователя */
  private void setMenu() {
    try {
      this.execute(
        new SetMyCommands(
          listOfCommands, new BotCommandScopeDefault(), null
        )
      );
    } catch (TelegramApiException e) {
      log.error("Возникла ошибка при настройке списка команд :\n" + e.getMessage());
    }
  }

  private void makeMenu() {
    listOfCommands.clear();

    if (userStatus.equals(UserStatus.Unknown)) {
      listOfCommands.add(new BotCommand("/start", "👉 Начать диалог") );
      helpText = "Бот дожен определить ваш статус в системе чтобы сформировать " +
        "соответствующий список команд." +
        "\n\n/start -> начать диалог с ботом.";
    }
    if (userStatus.equals(UserStatus.PotentialClient)) {
      listOfCommands.add(new BotCommand("/register", "💸 Записаться в зал."));
      helpText = "Поскольку вы не являетесь клиентом, то вам доступна только одна " +
        "команда:" +
        "\n\n/register -> стать клиентом нашего фитнес-клуба.";
    }
    else if (userStatus.equals(UserStatus.Client)) {
      listOfCommands.add(new BotCommand("/schedule", "🗓️ Расписание тренировок"));
      listOfCommands.add(new BotCommand("/coach", "🦾 Тренер"));
      helpText = "Вам доступны следующие команды :\n\n/schedule -> Получить информацию" +
        " о расписании тренировок или сформировать новое;" +
        "\n/coach -> Получить информацию о вашем тренере или записаться к другому.";
    }

    listOfCommands.add(new BotCommand("/help", "ℹ️ Информация о функционале программы" +
      "."));

    setMenu();

  }
  /* Отправка сообщений */
  private void justSendMessage(Long curChatId, String text,
                               Optional<InlineKeyboardMarkup> keyboard) {
    SendMessage message = new SendMessage();
    message.setChatId(curChatId);
    message.setText(text);
    message.setParseMode("Markdown");

    if (keyboard.isPresent()) {
      message.setReplyMarkup(keyboard.get());
    }

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
  private void sendEditedMessage(
    Long chatId,
    Integer mesId,
    String text,
    Optional<InlineKeyboardMarkup> keyboard
  )
  {
    EditMessageText editedMessage = new EditMessageText();
    editedMessage.setChatId(chatId);
    editedMessage.setMessageId(mesId);
    editedMessage.setText(text);
    if (keyboard.isPresent()) {
      editedMessage.setReplyMarkup(keyboard.get());
    }
    try {
      // Отправим сообщение
      execute(editedMessage);
    } catch (TelegramApiException e) {
      log.error("Error occurred : " + e.getMessage());
    }
  }

  /* Обработка событий */





}

