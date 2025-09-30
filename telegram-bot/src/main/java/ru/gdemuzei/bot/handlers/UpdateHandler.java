package ru.gdemuzei.bot.handlers;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface UpdateHandler {

    boolean supports(Update update);

    List<BotApiMethod<?>> handle(Update update);
}
