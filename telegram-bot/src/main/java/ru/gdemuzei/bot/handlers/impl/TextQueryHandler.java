package ru.gdemuzei.bot.handlers.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gdemuzei.bot.handlers.UpdateHandler;
import ru.gdemuzei.bot.service.MuseumPageLoader;

import java.util.List;

@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class TextQueryHandler implements UpdateHandler {

    private final MuseumPageLoader pageLoader;

    @Override
    public boolean supports(Update update) {
        if (!update.hasMessage()) {
            return false;
        }
        var msg = update.getMessage();
        return msg.hasText() && !msg.getText().startsWith("/");
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        long chatId = update.getMessage().getChatId();
        String q = update.getMessage().getText().trim();
        pageLoader.loadAndSendTextQuery(chatId, q, 0);
        return List.of();
    }
}
