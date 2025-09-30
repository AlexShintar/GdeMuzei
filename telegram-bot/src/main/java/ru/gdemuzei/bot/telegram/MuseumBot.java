package ru.gdemuzei.bot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.gdemuzei.bot.dispatching.UpdateDispatcher;

import java.util.List;

@Slf4j
@Component
public class MuseumBot implements SpringLongPollingBot {

    private final UpdateDispatcher dispatcher;

    private final String botToken;

    private final TelegramClient client;

    public MuseumBot(UpdateDispatcher dispatcher,
                     @Value("${bot.token}") String botToken,
                     TelegramClient client) {
        this.dispatcher = dispatcher;
        this.botToken = botToken;
        this.client = client;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updates -> updates.stream()
                .map(dispatcher::dispatch)
                .flatMap(List::stream)
                .forEach(action -> {
                    try {
                        client.execute(action);
                    } catch (TelegramApiException e) {
                        log.warn("Failed to execute {}: {}", action.getMethod(), e.getMessage(), e);
                    }
                });
    }
}
