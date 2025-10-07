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
@Order(20)
@RequiredArgsConstructor
public class LocationHandler implements UpdateHandler {

    private final MuseumPageLoader pageLoader;

    @Override
    public boolean supports(Update update) {
        if (!update.hasMessage()) {
            return false;
        }
        return update.getMessage().hasLocation();
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        var msg = update.getMessage();
        long chatId = msg.getChatId();
        var loc = msg.getLocation();
        pageLoader.loadAndSendPage(chatId, loc.getLatitude(), loc.getLongitude(), 0);
        return List.of();
    }
}
