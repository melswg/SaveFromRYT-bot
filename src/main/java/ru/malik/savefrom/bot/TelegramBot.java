package ru.malik.savefrom.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.DownloadManager;
import ru.malik.savefrom.util.FileCleaner;
import ru.malik.savefrom.util.LinkParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramBot extends TelegramLongPollingBot {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final DownloadManager downloadManager;

    private final Set<String> processingMessages = ConcurrentHashMap.newKeySet();

    public TelegramBot() {
        this.downloadManager = new DownloadManager();
    }

    public TelegramBot(DefaultBotOptions options) {
        super(options);
        this.downloadManager = new DownloadManager();
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
    }

    @Override
    public String getBotUsername() {
        return System.getenv("BOT_NAME");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();

            if (text.equals("/start")) {
                sendWelcomeMessage(message);
                return;
            }

            if (text.equals("/info")){
                sendInfoMessage(message);
                return;
            }

            String url = LinkParser.extractUrl(text);

            if (url != null) {
                String uniqueId = message.getChatId() + "_" + message.getMessageId(); //Уникальный ID задачи

                if (processingMessages.contains(uniqueId)) {
                    log.info("Дубликат запроса пропущен: {}", uniqueId);
                    return;
                }

                processingMessages.add(uniqueId);
                log.info("Получена ссылка: {}", url);

                executorService.submit(() -> processRequest(message, url, uniqueId));
            }
        }
    }

    private void processRequest(Message message, String url, String uniqueId) {
        MediaContent content = null;
        try {
            content = downloadManager.download(url);

            if (content == null || content.getFiles().isEmpty()) {
                log.warn("Контент не найден или пуст: {}", url);
                return;
            }

            List<File> files = content.getFiles();

            if (files.size() == 1) {
                File file = files.get(0);
                if (file.getName().endsWith(".mp4")) {
                    sendVideoContent(message, file, url);
                } else {
                    sendPhotoContent(message, file, url);
                }
            } else {
                sendAlbumContent(message, files, url);
            }

            deleteMessage(message);

        } catch (Exception e) {
            log.error("Ошибка при обработке запроса: ", e);
        } finally {
            if (content != null && !content.getFiles().isEmpty()) {
                FileCleaner.cleanup(content.getFiles().get(0).getParentFile());
            }
            processingMessages.remove(uniqueId);
        }
    }

    private void deleteMessage(Message message) {
        DeleteMessage delete = new DeleteMessage();
        delete.setChatId(message.getChatId().toString());
        delete.setMessageId(message.getMessageId());
        try {
            execute(delete);
        } catch (TelegramApiException e) {
            log.error("Не удалось удалить сообщение: {}", e.getMessage());
        }
    }

    private void sendVideoContent(Message message, File videoFile, String url) throws TelegramApiException {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(message.getChatId().toString());
        sendVideo.setVideo(new InputFile(videoFile));
        sendVideo.setParseMode(ParseMode.HTML);

        String safeUserName = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : "Незнакомец";
        String caption = String.format("Видео от @%s\n\n<a href=\"%s\">Источник</a>", safeUserName, url);
        sendVideo.setCaption(caption);

        execute(sendVideo);
        log.info("Видео отправлено в чат {}", message.getChatId());
    }

    private void sendPhotoContent(Message message, File photoFile, String url) throws TelegramApiException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(message.getChatId().toString());
        sendPhoto.setPhoto(new InputFile(photoFile));
        sendPhoto.setParseMode(ParseMode.HTML);

        String safeUserName = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : "Незнакомец";
        String caption = String.format("Фото от @%s\n\n<a href=\"%s\">Источник</a>", safeUserName, url);
        sendPhoto.setCaption(caption);

        execute(sendPhoto);
        log.info("Фото отправлено в чат {}", message.getChatId());
    }

    private void sendAlbumContent(Message message, List<File> files, String url) throws TelegramApiException {
        boolean isMultipart = files.size() > 10;
        int totalParts = (files.size() + 9) / 10;

        for (int i = 0; i < files.size(); i += 10) {
            int end = Math.min(i + 10, files.size());
            List<File> chunk = files.subList(i, end);

            List<InputMedia> mediaGroup = new ArrayList<>();
            String safeUserName = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : "Незнакомец";

            String partInfo = "";
            if (isMultipart) {
                int currentPart = (i / 10) + 1;
                partInfo = String.format(" (Часть %d/%d)", currentPart, totalParts);
            }

            String caption = String.format("Альбом%s от @%s\n\n<a href=\"%s\">Источник</a>",
                    partInfo, safeUserName, url);

            for (int j = 0; j < chunk.size(); j++) {
                File file = chunk.get(j);
                InputMedia media;
                if (file.getName().endsWith(".mp4")) {
                    media = new InputMediaVideo();
                } else {
                    media = new InputMediaPhoto();
                }

                media.setMedia(file, file.getName());

                if (j == 0) {
                    media.setCaption(caption);
                    media.setParseMode(ParseMode.HTML);
                }
                mediaGroup.add(media);
            }

            SendMediaGroup sendMediaGroup = new SendMediaGroup();
            sendMediaGroup.setChatId(message.getChatId().toString());
            sendMediaGroup.setMedias(mediaGroup);
            execute(sendMediaGroup);
        }
        log.info("Альбом из {} файлов отправлен в чат {}", files.size(), message.getChatId());
    }

    private void sendWelcomeMessage(Message message) {
        String text = """
                Привет! Я **SaveFromRYT Bot** - твой карманный помощник для скачивания видео. 🤖
                
                Я помогу тебе сохранить контент из различных социальных сетей без лишних хлопот.
                
                Как пользоваться? Просто отправь мне ссылку на видео, а я пришлю тебе файл.
                
                Поддерживаемые платформы:
                 TikTok
                 Instagram
                 YouTube
                 RuTube
                 Twitch
                
                Попробуй прямо сейчас! Просто отправь мне ссылку.
                """;

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);
        sendMessage.setParseMode(ParseMode.MARKDOWN);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки приветствия: ", e);
        }
    }

    private void sendInfoMessage(Message message){
        String text = """
                **Информация** 🛠
                
                Как скачивать: Скопируй ссылку из приложения (TikTok/YT/Insta/...) и вставь её в чат со мной.
                
                Если не работает: Убедись, что профиль автора видео открыт (приватные видео я скачать не смогу).
                
                Формат: Я стараюсь присылать видео в максимально возможном качестве.
                
                👨‍💻 Разработчик: @itAlm0stWorked (по предложениям и вопросам), github.com/melswg
                
                Если бот столкнулся с ошибкой, попробуй отправить ссылку еще раз через минуту.
                """;

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);
        sendMessage.setParseMode(ParseMode.MARKDOWN);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e){
            log.error("Ошибка при отправке информации: ", e);
        }
    }


}