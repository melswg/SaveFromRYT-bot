package ru.malik.savefrom;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.malik.savefrom.bot.TelegramBot;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            botsApi.registerBot(new TelegramBot());

            System.out.println("Бот запущен и готов к работе");
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
}
