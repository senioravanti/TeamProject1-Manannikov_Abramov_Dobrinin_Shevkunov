package ru.stradiavanti.train_plan_bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

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
    Unknown, PotentialClient, Client, Coach
  }
  @Autowired
  // Включаем зависимость и внедряем ее
  private UserRepository userRepository;
  @Autowired
  private TrainerRepository trainerRepository;
  private UserStatus userStatus;
  private final BotConfig config;
  // Часто используемые свойства, связанные с пользователем
  private Long chatId;
  private Integer sentMessageId;
  private Long sentChatId;
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

  // Флаги
  boolean choosingCoach;
  boolean changesCoachRating;
  // ---
  private final String HELP_TEXT = "\n***\n\nМои ф-ции :\n" +
    "- Составить расписание /getschedule" +
    "тренировок\n- Записать к тренеру : /setcoach\n- Отсылать тебе оповещения пока ты не" +
    " начнешь регулярно" +
    "ходить на" +
    " трени (или не добавишь меня в черный список).\n\n***";



  public TelegramBot(BotConfig config) {
    super(config.getToken());
    this.config = config;
    choosingCoach = false;
    changesCoachRating = false;

    userStatus = UserStatus.Unknown;
    // Реализуем меню
    makeMenu();
    setMenu();

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

      if (!choosingCoach && !changesCoachRating) {
        switch (userMessageText) {
          case "/start":
            //registerUser(update.getMessage());
            startCommandReceived();

            break;
          case "/register":
            // Выносим все в отдельные методы
            register();
            break;
          case "/coach":
            coach();
            break;
          case "/schedule":
            schedule();
            break;
          case "/help":
            // Выводим список команд.
            justSendMessage(chatId, HELP_TEXT);
            log.info("Replied to user " + curUserName);

            break;
          default:
            justSendMessage(chatId, "😬 Я не знаю что делать, такая команда в меня не " +
              "заложена");
        }
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
            User user = userRepository.findById(chatId).get();
            user.setTrainerId(trainer.get().getId());

            botMessageText =
              "🎉 Поздравляю " + name.get(0) + " " + name.get(1) + " " + name.get(2) +
              " - " +
              "ваш новый тренер.";
            trainer.get().setIsFree(false);

            trainerRepository.save(trainer.get());
            userRepository.save(user);

          } else {
            botMessageText = "🤔 Хм, в нашей базе данных нет тренера с таким ФИО.";
          }
        }
        botMessage.setText(botMessageText);
        sendMessage(botMessage);
        choosingCoach = false;
      }
      else if (changesCoachRating) {
        try {
          Double newRating = Double.parseDouble(userMessageText);
          updateCoachRating("✍️ Обновляю рейтинг тренера ...", newRating);
        } catch (NumberFormatException e) {
          justSendMessage(chatId,"😬 Вы ввели строку вместо числа !");
        }
        changesCoachRating = false;
      }
    // Нажали на кнопку.
    }
    else if (update.hasCallbackQuery()) {
      CallbackQuery callbackQuery = update.getCallbackQuery();

      Message sentMessage = callbackQuery.getMessage();
      String data = callbackQuery.getData();

      sentMessageId = sentMessage.getMessageId();
      sentChatId = sentMessage.getChatId();

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
        setMenu();

        sendEditedMessage(sentChatId, sentMessageId, editedMessageText, Optional.empty());

      } else if (data.equals(__REGISTER_CLIENT_BUTTON)) {
        // Будем регистрировать
        register();

      } else if (data.equals(__REJECT_SUBSCRIPTION_BUTTON)) {
        editedMessageText = "🤑 Пожалуйста подумайте еще раз, очень хотим вас видеть в нашем клубе.";
        sendEditedMessage(sentChatId, sentMessageId, editedMessageText, Optional.empty());

      } else if (data.equals(__COACHES_LIST_BUTTON)) {
        editedMessageText = getCoachesList();

        sendEditedMessage(
          sentChatId,
          sentMessageId,
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
        justSendMessage(sentChatId, "✍️ Введи ФИО тренера, из списка выше, между " +
          "словами " +
          "ставь пробелы, не пиши лишнего !");
        choosingCoach = true;


      }
      else if (data.equals(__REJECT_COACH_BUTTON)) {
        editedMessageText = "😬 Очень жаль, специально для тебя мы обязательно наймем " +
          "Арнольда Шварцнегера.";

        sendEditedMessage(
          sentChatId,
          sentMessageId,
          editedMessageText,
          Optional.empty()
        );
      }
      else if (data.equals(__CHANGE_COACH_BUTTON)) {
        User client = userRepository.findById(sentChatId).get();
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
          sentChatId,
          sentMessageId,
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
        User client = userRepository.findById(sentChatId).get();
        Trainer trainer = trainerRepository.findById(client.getTrainerId()).get();

        String trainerInfo =
          "\nИнформация о твоем тренере\n\n- ФИО : " + trainer.getLastName() + " " + trainer.getFirstName() + " " + trainer.getFatherName() +
          "\n- Специализация : " + trainer.getSpecialization() +
          String.format("\n- Рейтинг : %.2f", getCoachRating(
            trainer.getSumOfVotes(),
            trainer.getNumberOfVotes()
          ));

        SendPhoto botPhotoMessage = new SendPhoto();
        botPhotoMessage.setChatId(sentChatId);
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

        sendEditedMessage(
          sentChatId,
          sentMessageId,
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
        updateCoachRating("🤘 Мы его уволим, не волнуйтесь.",
          1.0);
        changesCoachRating = false;
      }
      else if (data.equals(__SET_COACH_RATING_TWO_STAR)){
        updateCoachRating("✍️ Зафиксировал, лично сделаю тренеру последнее китайское...",
          2.0);
        changesCoachRating = false;

      }
      else if (data.equals(__SET_COACH_RATING_THREE_STAR)){
        updateCoachRating("😬 Понял, скажу начальнику конторы чтобы поговорил с ним.",
          3.0);
        changesCoachRating = false;

      }
      else if (data.equals(__SET_COACH_RATING_FOUR_STAR)){
        updateCoachRating("🎉 Рад что вы оценили нашего тренера !", 4.0);
        changesCoachRating = false;

      }
      else if (data.equals(__SET_COACH_RATING_FIVE_STAR)){
        updateCoachRating("🤩 Выдам вашему тренеру годовую зарплату в качестве премии !",
          5.0);
        changesCoachRating = false;

      }
    }

  }

  private Double getCoachRating(Double sumOfVotes, Integer numberOfVotes) {
    if (numberOfVotes == null || numberOfVotes == 0) {
      return 0.0;
    } else {
      return sumOfVotes / numberOfVotes;
    }
  }
  private void updateCoachRating(String messageText, Double newRating) {
    justSendMessage(
      sentChatId,
      messageText + "\nОбновленный рейтинг : " + updateCoachRatingInTable(newRating).toString()
    );
  }
  private Double updateCoachRatingInTable(Double rating) {
    Double newRating;

    Trainer trainer =
      trainerRepository.findById(userRepository.findById(sentChatId).get().getTrainerId()).get();

    Double curSumOfVotes = trainer.getSumOfVotes();
    Integer curNumberOfVotes = trainer.getNumberOfVotes();
    if (curSumOfVotes == null) curSumOfVotes = 0.0;

    trainer.setSumOfVotes(curSumOfVotes += rating);
    trainer.setNumberOfVotes(++curNumberOfVotes);

    newRating = getCoachRating(trainer.getSumOfVotes(), trainer.getNumberOfVotes());

    trainerRepository.save(trainer);

    return newRating;
  }
  private void coach() {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);

    if (userRepository.findById(chatId).get().getTrainerId() == -1) {
      message.setReplyMarkup(
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
      message.setText("🧙‍♂️ На данный момент у вас не тренера, вы занимаетесь " +
        "самостоятельно, рекомендую вам выбрать тренера с самым высоким рейтингом !");
    } else {
      message.setReplyMarkup(
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
      message.setText("️🤔 У тебя уже есть крутейший наставник, чего же тебе еще надо ?");
    }
    sendMessage(message);
  }
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

  // TODO Настроить расписание
  private void schedule() {
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
           getCoachRating(
             it.getSumOfVotes(),
             it.getNumberOfVotes()
           )
         ));

      if (it.getIsFree()) coachesList = coachesList.concat(" 🟩\n\n");
      else coachesList = coachesList.concat(" 🟥\n\n");
      i++;
    }
    return coachesList;

  }

  private void makeMenu() {
    listOfCommands = new ArrayList<BotCommand>();

    if (userStatus.equals(UserStatus.Unknown)) {
      listOfCommands.add(new BotCommand("/start", "👉 Начните диалог с ботом чтобы он " +
        "определил ваш статут в системе, и подобрал наиболее полезные для вас команды."));
    }
    if (userStatus.equals(UserStatus.PotentialClient)) {
      listOfCommands.add(new BotCommand("/register", "💸 Записаться в зал."));
    } else if (userStatus.equals(UserStatus.Client)) {
      listOfCommands.add(new BotCommand("/schedule", "🗓️ Расписание тренировок"));
      listOfCommands.add(new BotCommand("/coach", "🦾 Тренер"));
    } else if (userStatus.equals(UserStatus.Coach)) {
      listOfCommands.add(new BotCommand("/schedule", "🗓️ Расписание занятий"));
      listOfCommands.add(new BotCommand("/clients", "📃 Список клиентов"));
    }

    listOfCommands.add(new BotCommand("/help", "🆘 Поясни за функционал"));


  }

  private void justSendMessage(Long curChatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(curChatId);
    message.setText(text);
    message.setParseMode("Markdown");
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

  private void startCommandReceived() {

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

    } else {
      answer = "🫡 Здравия желаю " + curUserName + " !\n\nЯ тебя узнал, следовательно " +
        "помогу" +
        " " +
        "тебе составить новое " +
        "расписание тренировок, или выведу действующее.";
      userStatus = UserStatus.Client;
      makeMenu();
      setMenu();
    }

    message.setText(answer);
    sendMessage(message);

    log.info("Replied to user " + curUserName);
  }

  private void register() {
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
    user.setStartSubscriptionDate(LocalDate.now());
    user.setEndSubscriptionDate(user.getStartSubscriptionDate().plusMonths(3));
    // Последний параметр надо из таблицы брать.
    userRepository.save(user);
    // Оператор + -> перегруженный метод toString
    log.info("User saved" + user);
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

}
