package ru.malik.savefrom.bot;

import org.glassfish.grizzly.utils.EchoFilter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.DownloadManager;
import ru.malik.savefrom.util.FileCleaner;
import ru.malik.savefrom.util.LinkParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class TelegramBot extends TelegramLongPollingBot {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final DownloadManager downloadManager;

    public TelegramBot() {
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
        LinkParser lp = new LinkParser();
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

            if (content != null && content.isVideo()) {
                File videoFile = content.getFiles().get(0);

                SendVideo sendVideo = new SendVideo();
                sendVideo.setChatId(message.getChatId().toString());
                sendVideo.setVideo(new InputFile(videoFile));

                sendVideo.setParseMode(ParseMode.HTML);

                String safeUserName = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : "Незнакомец";

                String caption = String.format("Ссылка от @%s\n\n<a href=\"%s\">Адрес ссылки</a>",
                        safeUserName, url);

                sendVideo.setCaption(caption);

                execute(sendVideo);
                log.info("Видео успешно отправлено в чат {}", message.getChatId());

                deleteMessage(message);
            } else {
                log.warn("Контент не найден или не поддерживается: {}", url);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса: ", e);
        } finally {
            if (content != null && !content.getFiles().isEmpty()){
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

}
