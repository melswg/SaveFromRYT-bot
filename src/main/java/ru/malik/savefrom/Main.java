package ru.malik.savefrom;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.malik.savefrom.bot.TelegramBot;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            DefaultBotOptions options = new DefaultBotOptions();

            String customUrl = System.getenv("BOT_API_URL");

            if(customUrl != null && !customUrl.isEmpty()){
                options.setBaseUrl(customUrl);
                System.out.println("Используется локальный сервер для Telegram, API: " + customUrl);
            } else {
                System.out.println("Используется стандартный сервер Telegram Cloud");
            }

            botsApi.registerBot(new TelegramBot(options));

            System.out.println("Бот запущен и готов к работе");
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
}
