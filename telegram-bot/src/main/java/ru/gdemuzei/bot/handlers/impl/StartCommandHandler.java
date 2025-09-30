package ru.gdemuzei.bot.handlers.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gdemuzei.bot.handlers.UpdateHandler;
import ru.gdemuzei.bot.telegram.TelegramMessageFactory;

import java.util.List;

@Order(10)
@Component
public class StartCommandHandler implements UpdateHandler {

    @Override
    public boolean supports(Update update) {
        return update.hasMessage()
                && update.getMessage().hasText()
                && "/start".equals(update.getMessage().getText());
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        long chatId = update.getMessage().getChatId();
        return List.of(TelegramMessageFactory.createStartMessage(chatId));
    }
}
