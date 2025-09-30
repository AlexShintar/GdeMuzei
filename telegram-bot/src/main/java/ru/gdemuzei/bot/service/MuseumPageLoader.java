package ru.gdemuzei.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.gdemuzei.bot.client.MuseumApiClient;
import ru.gdemuzei.bot.repositories.MuseumCache;
import ru.gdemuzei.bot.telegram.TelegramMessageFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class MuseumPageLoader {

    private static final int PAGE_LIMIT = 10;

    private final MuseumApiClient searchClient;

    private final TelegramClient client;

    private final MuseumCache cache;

    public void loadAndSendPage(long chatId, double lat, double lon, int offset) {
        AtomicInteger count = new AtomicInteger(0);
        searchClient.search(lat, lon, offset, PAGE_LIMIT)
                .take(PAGE_LIMIT)
                .doOnError(err -> {
                    log.error("Failed to load museums for chat_id={}", chatId, err);
                    send(TelegramMessageFactory.createErrorMessage(chatId,
                            "Не удалось получить список музеев, попробуйте позже."));
                })
                .doOnNext(dto -> {
                    cache.put(chatId, dto);
                    send(TelegramMessageFactory.createListItemMessage(chatId, dto));
                    count.incrementAndGet();
                })
                .doOnComplete(() -> {
                    if (count.get() == PAGE_LIMIT) {
                        send(TelegramMessageFactory.createNextButtonMessage(
                                chatId, lat, lon, offset + PAGE_LIMIT));
                    }
                })
                .subscribe();
    }

    private void send(BotApiMethod<?> method) {
        try {
            client.execute(method);
        } catch (TelegramApiException e) {
            log.warn("Telegram send failed: {}", e.getMessage(), e);
        }
    }
}
