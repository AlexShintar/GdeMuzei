package ru.gdemuzei.bot.handlers.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gdemuzei.contracts.MuseumSummaryDto;
import ru.gdemuzei.bot.handlers.UpdateHandler;
import ru.gdemuzei.bot.repositories.MuseumCache;
import ru.gdemuzei.bot.telegram.TelegramMessageFactory;

import java.util.List;

@Component
@Order(30)
@RequiredArgsConstructor
public class InfoHandler implements UpdateHandler {

    private final MuseumCache cache;

    @Override
    public boolean supports(Update update) {
        return update.hasCallbackQuery()
                && update.getCallbackQuery().getData().startsWith("info:");
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String id = update.getCallbackQuery().getData().substring("info:".length());
        MuseumSummaryDto dto = cache.get(id);

        if (dto == null) {
            return List.of(TelegramMessageFactory.createErrorMessage(chatId,
                    "Данные устарели. Повторите поиск."));
        }
//        dto = apiClient.getById(id).blockOptional().orElse(null);
//        if (dto != null) cache.put(dto);
        return List.of(TelegramMessageFactory.createInfoMessage(chatId, dto),
                TelegramMessageFactory.createVenueMessage(chatId, dto));
    }
}
