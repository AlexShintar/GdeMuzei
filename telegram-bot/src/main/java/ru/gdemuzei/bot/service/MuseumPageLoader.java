package ru.gdemuzei.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.gdemuzei.bot.client.MuseumApiClient;
import ru.gdemuzei.bot.repositories.MuseumCache;
import ru.gdemuzei.bot.telegram.TelegramMessageFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MuseumPageLoader {

    private static final int PAGE_LIMIT = 10;

    /** Пауза между сообщениями для защиты от flood-limit */
    private static final Duration MSG_SPACING = Duration.ofMillis(350);

    private final MuseumApiClient searchClient;

    private final TelegramClient client;

    private final MuseumCache cache;

    public void loadAndSendPage(long chatId, double lat, double lon, int offset) {
        AtomicInteger count = new AtomicInteger(0);
        searchClient.search(lat, lon, offset, PAGE_LIMIT)
                .take(PAGE_LIMIT)
                .delayElements(MSG_SPACING)
                .concatMap(dto ->
                        sendMono(TelegramMessageFactory.createListItemMessage(chatId, dto))
                                .doOnSuccess(v -> {
                                    cache.put(dto);
                                    count.incrementAndGet();
                                })
                )
                .then(Mono.defer(() -> {
                    if (count.get() == PAGE_LIMIT) {
                        return sendMono(TelegramMessageFactory.createNextButtonMessage(
                                chatId, lat, lon, offset + PAGE_LIMIT));
                    }
                    return Mono.empty();
                }))
                .onErrorResume(err -> {
                    log.error("Failed to load museums for chat_id={}", chatId, err);
                    return sendMono(TelegramMessageFactory.createErrorMessage(chatId,
                            "Не удалось получить список музеев, попробуйте позже."));
                })
                .subscribe();
    }

    public void loadAndSendTextQuery(long chatId, String query, int offset) {
        AtomicInteger count = new AtomicInteger(0);

        searchClient.searchText(query, offset, PAGE_LIMIT)
                .take(PAGE_LIMIT)
                .delayElements(MSG_SPACING)
                .concatMap(dto ->
                        sendMono(TelegramMessageFactory.createListItemMessage(chatId, dto))
                                .doOnSuccess(v -> {
                                    cache.put(dto);
                                    count.incrementAndGet();
                                })
                )
                .then(Mono.defer(() -> {
                    if (count.get() == PAGE_LIMIT) {
                        return sendMono(TelegramMessageFactory.createNextTextButtonMessage(
                                chatId, query, offset + PAGE_LIMIT));
                    } else if (count.get() == 0) {
                        return sendMono(TelegramMessageFactory.createHintMessage(chatId));
                    }
                    return Mono.empty();
                }))
                .onErrorResume(err -> {
                    log.error("Text search failed: chat_id={}, q={}", chatId, query, err);
                    return sendMono(TelegramMessageFactory.createErrorMessage(chatId,
                            "Не удалось выполнить поиск. Попробуйте позже."));
                })
                .subscribe();
    }

    /* ---------------- Отправка с учётом flood-control ---------------- */
    private Mono<Void> sendMono(BotApiMethod<?> method) {
        return Mono.fromCallable(() -> client.execute(method))
                .then()
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> handleTelegramError(e, method));
    }

    private Mono<Void> handleTelegramError(Throwable e, BotApiMethod<?> method) {
        if (e instanceof TelegramApiRequestException tre) {
            long retryAfter = parseRetryAfterSeconds(tre.getApiResponse());
            if (tre.getErrorCode() == 429 && retryAfter > 0) {
                log.warn("Flood control: retry after {}s", retryAfter);
                return Mono.delay(Duration.ofSeconds(retryAfter))
                        .then(Mono.fromCallable(() -> client.execute(method)).then())
                        .subscribeOn(Schedulers.boundedElastic());
            }
        }
        log.warn("Telegram send failed: {}", e.getMessage(), e);
        return Mono.empty();
    }

    private long parseRetryAfterSeconds(String apiResponse) {
        if (apiResponse == null) return 0;
        var m = Pattern.compile("(retry_after|retryAfter)\\D+(\\d+)").matcher(apiResponse);
        return m.find() ? Long.parseLong(m.group(2)) : 0;
    }
}
