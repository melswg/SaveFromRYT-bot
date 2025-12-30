package ru.malik.savefrom.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramBot extends TelegramLongPollingBot {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final DownloadManager downloadManager;

    public TelegramBot() {
        this.downloadManager = new DownloadManager();
    }

    public TelegramBot(DefaultBotOptions options) {
        super(options);
        this.downloadManager = new DownloadManager();
    }

    public String getBotToken(){
        return System.getenv("BOT_TOKEN");
    }

    @Override
    public String getBotUsername(){
        return System.getenv("BOT_NAME");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            String message = update.getMessage().getText();
            String url = LinkParser.extractUrl(message);

            if (url != null){
                log.info("Получена ссылка на скачивание: {}", url);
                executorService.submit(() ->
                        processRequest(update.getMessage(), url));
            } else {
                System.out.println("Сообщение есть, но ссылки в нем нет.");
            }
        };
    }

    private void processRequest(Message message, String url) {
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
        }
    }

    private void deleteMessage(Message message){
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
        String caption = String.format("Фото от @%s\n<a href=\"%s\">Источник</a>", safeUserName, url);
        sendPhoto.setCaption(caption);

        execute(sendPhoto);
        log.info("Фото отправлено в чат {}", message.getChatId());
    }

    private void sendAlbumContent(Message message, List<File> files, String url) throws TelegramApiException {
        // Разбиваем список на куски по 10
        for (int i = 0; i < files.size(); i += 10) {
            int end = Math.min(i + 10, files.size());
            List<File> chunk = files.subList(i, end);

            List<InputMedia> mediaGroup = new ArrayList<>();
            String safeUserName = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : "Незнакомец";
            String caption = String.format("Альбом (%d/%d) от @%s\n<a href=\"%s\">Источник</a>",
                    (i/10)+1, (files.size()-1)/10 + 1, safeUserName, url);

            for (int j = 0; j < chunk.size(); j++) {
                File file = chunk.get(j);
                InputMedia media;
                if (file.getName().endsWith(".mp4")) {
                    media = new InputMediaVideo();
                } else {
                    media = new InputMediaPhoto();
                }

                // Исправили имя файла!
                media.setMedia(file, file.getName());

                // Подпись только к первому медиа в ГРУППЕ
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
}
