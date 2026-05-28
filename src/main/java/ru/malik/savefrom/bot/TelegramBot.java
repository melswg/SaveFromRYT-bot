package ru.malik.savefrom.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;
import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.ContentTooLargeException;
import ru.malik.savefrom.service.DownloadManager;
import ru.malik.savefrom.util.FileCleaner;
import ru.malik.savefrom.util.LinkParser;
import ru.malik.savefrom.util.VideoInfoExtractor;

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

    private static final int ALBUM_SEND_MAX_ATTEMPTS = 3;
    private static final long ALBUM_SEND_BASE_DELAY_MS = 2000L;

    private static final String LOADING_TEXT = "⏳";
    private static final String ERROR_TEXT = """
            **Не удалось получить контент** 😔
            
            Возможные причины:
            • Ограниченный доступ (приватный профиль/видео).
            • Возрастные или региональные ограничения.
            • Ошибка при получении данных.
            
            Попробуй еще раз через минуту.
            """;

    private static final String TOO_LARGE_TEXT = """
            **Видео слишком большое** 😔
            
            Я попробовал получить видео в ограниченном формате и, если нужно, аудиоверсию, но файл всё равно не помещается в лимит отправки.
            
            Попробуй отправить более короткое видео или другую ссылку.
            """;

    private static final String WELCOME_TEXT = """
            Привет! Я SaveFromRYT Bot - твой карманный помощник для скачивания видео. 🤖
            
            Я помогу тебе сохранить контент из различных социальных сетей без лишних хлопот.
            
            Как пользоваться? Просто отправь мне ссылку на видео, а я пришлю тебе файл.
            
            Поддерживаемые платформы:
            TikTok
            Instagram
            YouTube
            RuTube
            Twitch
            И не только, список постоянно пополняется!
            
            Попробуй прямо сейчас! Просто отправь мне ссылку.
            """;

    private static final String INFO_TEXT = """
            Информация 🛠
            
            Как скачивать: Скопируй ссылку из приложения (TikTok/YT/Insta/...) и вставь её в чат со мной.
            
            Если не работает: Убедись, что профиль автора видео открыт (приватные видео я скачать не смогу).
            
            Формат: Я стараюсь присылать видео в максимально возможном качестве.
            
            👨‍💻 Разработчик: @itAlm0stWorked, github.com/melswg
            
            Если бот столкнулся с ошибкой, попробуй отправить ссылку еще раз через минуту.
            """;

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
                sendTextMessage(message.getChatId(), WELCOME_TEXT);
                return;
            }

            if (text.equals("/info")){
                sendTextMessage(message.getChatId(), INFO_TEXT);
                return;
            }

            String url = LinkParser.extractUrl(text);

            if (url != null) {
                String uniqueId = message.getChatId() + "_" + message.getMessageId();

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
        Message loadingMessage = null;

        try {
            loadingMessage = sendLoadingMessage(message.getChatId());

            content = downloadManager.download(url);

            if (content == null || content.getFiles().isEmpty()) {
                log.warn("Контент не найден или пуст: {}", url);
                sendTextMessage(message.getChatId(), ERROR_TEXT);
                return;
            }

            List<File> files = content.getFiles();

            if (files.size() == 1) {
                File file = files.get(0);
                String name = file.getName().toLowerCase();

                if (name.endsWith(".mp4") || name.endsWith(".webm")) {
                    sendVideoContent(message, file, url, content.getSource());
                } else if (isAudioFile(name)) {
                    sendAudioContent(message, file, url);
                } else if (file.getName().endsWith(".gif")) {
                    sendAnimationContent(message, file, url);
                } else {
                    sendPhotoContent(message, file, url);
                }
            } else {
                sendAlbumContent(message, files, url, content.getSource());
            }

            deleteUserMessage(message);

        } catch (ContentTooLargeException e) {
            log.warn("Контент слишком большой для отправки: {}", url, e);
            sendTextMessage(message.getChatId(), TOO_LARGE_TEXT);
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса: ", e);
            sendTextMessage(message.getChatId(), ERROR_TEXT);
        } finally {
            deleteSystemMessage(loadingMessage);

            if (content != null && !content.getFiles().isEmpty()) {
                FileCleaner.cleanup(content.getFiles().get(0).getParentFile());
            }
            processingMessages.remove(uniqueId);
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        sendMessage.setParseMode(ParseMode.MARKDOWN);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить текстовое сообщение: ", e);
        }
    }

    private Message sendLoadingMessage(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(LOADING_TEXT);
        try {
            return execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение о загрузке", e);
            return null;
        }
    }

    private void deleteUserMessage(Message message) {
        if (message == null) return;
        if (message.getChat().isUserChat()) {
            return;
        }
        deleteSystemMessage(message);
    }

    private void deleteSystemMessage(Message message) {
        if (message == null) return;

        DeleteMessage delete = new DeleteMessage();
        delete.setChatId(message.getChatId().toString());
        delete.setMessageId(message.getMessageId());
        try {
            execute(delete);
        } catch (TelegramApiException e) {
            log.warn("Не удалось удалить сообщение {}: {}", message.getMessageId(), e.getMessage());
        }
    }

    private String getCaption(Message message, String url, String type) {
        String sourceLink = String.format("<a href=\"%s\">Источник</a>", url);

        if (message.getChat().isUserChat()) {
            return String.format("%s | %s", type, sourceLink);
        }

        String safeUserName = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : "Незнакомец";
        return String.format("%s от @%s\n\n%s", type, safeUserName, sourceLink);
    }

    private void sendVideoContent(Message message, File videoFile, String url, String source) throws TelegramApiException {
        File fileToSend = videoFile;

        if ("YTDLP".equals(source)) {
            fileToSend = FileCleaner.fastFixVideo(videoFile);
        }

        VideoInfoExtractor.VideoMetadata metadata = VideoInfoExtractor.getMetadata(fileToSend);
        File thumbnail = VideoInfoExtractor.extractThumbnail(fileToSend);

        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(message.getChatId().toString());
        sendVideo.setVideo(new InputFile(fileToSend));
        sendVideo.setSupportsStreaming(true);

        if (thumbnail != null) {
            sendVideo.setThumb(new InputFile(thumbnail));
        }

        sendVideo.setParseMode(ParseMode.HTML);

        if (metadata != null) {
            if (metadata.width() > 0) sendVideo.setWidth(metadata.width());
            if (metadata.height() > 0) sendVideo.setHeight(metadata.height());
            if (metadata.duration() > 0) sendVideo.setDuration(metadata.duration());
            log.info("Отправка видео с метаданными: {}x{}, {} сек.", metadata.width(), metadata.height(), metadata.duration());
        }

        String caption = getCaption(message, url, "Видео");
        sendVideo.setCaption(caption);

        try {
            execute(sendVideo);
            log.info("Видео отправлено в чат {}", message.getChatId());
        } finally {
            if (thumbnail != null && thumbnail.exists()) {
                thumbnail.delete();
            }
        }
    }

    private void sendPhotoContent(Message message, File photoFile, String url) throws TelegramApiException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(message.getChatId().toString());
        sendPhoto.setPhoto(new InputFile(photoFile));
        sendPhoto.setParseMode(ParseMode.HTML);

        String caption = getCaption(message, url, "Фото");
        sendPhoto.setCaption(caption);

        execute(sendPhoto);
        log.info("Фото отправлено в чат {}", message.getChatId());
    }

    private void sendAlbumContent(Message message, List<File> files, String url, String source) throws TelegramApiException {
        boolean isMultipart = files.size() > 10;
        int totalParts = (files.size() + 9) / 10;

        for (int i = 0; i < files.size(); i += 10) {
            int end = Math.min(i + 10, files.size());
            List<File> chunk = files.subList(i, end);

            List<InputMedia> mediaGroup = new ArrayList<>();
            List<File> mediaGroupFiles = new ArrayList<>();
            String safeUserName = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : "Незнакомец";

            String partInfo = "";
            if (isMultipart) {
                int currentPart = (i / 10) + 1;
                partInfo = String.format(" (Часть %d/%d)", currentPart, totalParts);
            }

            String baseCaption = getCaption(message, url, "Альбом" + partInfo);

            for (int j = 0; j < chunk.size(); j++) {
                File file = chunk.get(j);
                InputMedia media;
                if (file.getName().endsWith(".mp4")) {
                    media = new InputMediaVideo();
                    File fileToSend = file;
                    if ("YTDLP".equals(source)) {
                        fileToSend = FileCleaner.fastFixVideo(file);
                    }
                    media.setMedia(fileToSend, fileToSend.getName());

                } else if (isAudioFile(file.getName())) {
                    sendAudioContent(message, file, url);
                    continue;
                } else if (file.getName().toLowerCase().endsWith(".gif")) {
                    sendAnimationContent(message, file, url);
                    continue;
                } else {
                    media = new InputMediaPhoto();
                    media.setMedia(file, file.getName());
                }

                if (j == 0) {
                    media.setCaption(baseCaption);
                    media.setParseMode(ParseMode.HTML);
                }
                mediaGroup.add(media);
                mediaGroupFiles.add(file);
            }

            if (mediaGroup.size() == 1) {
                sendSingleContent(message, mediaGroupFiles.get(0), url, source);
            } else if (!mediaGroup.isEmpty()) {
                SendMediaGroup sendMediaGroup = new SendMediaGroup();
                sendMediaGroup.setChatId(message.getChatId().toString());
                sendMediaGroup.setMedias(mediaGroup);
                executeMediaGroupWithRetry(sendMediaGroup);
            }

            if (i + 10 < files.size()) {
                try {
                    log.info("Пауза 2 секунды перед отправкой следующей части альбома...");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        log.info("Альбом из {} файлов отправлен в чат {}", files.size(), message.getChatId());
    }

    private void executeMediaGroupWithRetry(SendMediaGroup sendMediaGroup) throws TelegramApiException {
        for (int attempt = 1; attempt <= ALBUM_SEND_MAX_ATTEMPTS; attempt++) {
            try {
                execute(sendMediaGroup);
                return;
            } catch (TelegramApiValidationException e) {
                throw e;
            } catch (TelegramApiException e) {
                if (attempt == ALBUM_SEND_MAX_ATTEMPTS) {
                    throw e;
                }

                long delayMs = getAlbumRetryDelayMs(e, attempt);
                log.warn("Не удалось отправить альбом, попытка {}/{}. Повтор через {} мс: {}",
                        attempt, ALBUM_SEND_MAX_ATTEMPTS, delayMs, e.getMessage());
                sleepBeforeAlbumRetry(delayMs);
            }
        }
    }

    private void sendSingleContent(Message message, File file, String url, String source) throws TelegramApiException {
        String name = file.getName().toLowerCase();

        if (name.endsWith(".mp4") || name.endsWith(".webm")) {
            sendVideoContent(message, file, url, source);
        } else if (isAudioFile(name)) {
            sendAudioContent(message, file, url);
        } else if (name.endsWith(".gif")) {
            sendAnimationContent(message, file, url);
        } else {
            sendPhotoContent(message, file, url);
        }
    }

    private long getAlbumRetryDelayMs(TelegramApiException e, int attempt) {
        if (e instanceof TelegramApiRequestException requestException
                && requestException.getParameters() != null
                && requestException.getParameters().getRetryAfter() != null) {
            return requestException.getParameters().getRetryAfter() * 1000L + 500L;
        }

        return ALBUM_SEND_BASE_DELAY_MS * (1L << (attempt - 1));
    }

    private void sleepBeforeAlbumRetry(long delayMs) throws TelegramApiException {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelegramApiException("Interrupted while waiting to retry media group", e);
        }
    }

    private void sendAnimationContent(Message message, File file, String url) throws TelegramApiException {
        SendAnimation sendAnimation = new SendAnimation();
        sendAnimation.setChatId(message.getChatId().toString());
        sendAnimation.setAnimation(new InputFile(file));
        sendAnimation.setCaption(getCaption(message, url, "GIF"));
        sendAnimation.setParseMode(ParseMode.HTML);
        execute(sendAnimation);
    }

    private void sendAudioContent(Message message, File audioFile, String url) throws TelegramApiException {
        SendAudio sendAudio = new SendAudio();
        sendAudio.setChatId(message.getChatId().toString());
        sendAudio.setAudio(new InputFile(audioFile));

        sendAudio.setParseMode(ParseMode.HTML);
        String caption = getCaption(message, url, "Аудио");
        sendAudio.setCaption(caption);

        execute(sendAudio);
        log.info("Аудио отправлено в чат {}", message.getChatId());
    }

    private boolean isAudioFile(String name) {
        String lowerName = name.toLowerCase();
        return lowerName.endsWith(".mp3")
                || lowerName.endsWith(".m4a")
                || lowerName.endsWith(".opus")
                || lowerName.endsWith(".wav");
    }

}
