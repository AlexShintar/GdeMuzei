package ru.gdemuzei.bot.handlers.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gdemuzei.bot.handlers.UpdateHandler;
import ru.gdemuzei.bot.service.MuseumPageLoader;
import ru.gdemuzei.bot.telegram.TelegramMessageFactory;
import ru.gdemuzei.bot.util.GeoUtil;

import java.util.List;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class CoordsHandler implements UpdateHandler {

    private final MuseumPageLoader pageLoader;

    @Override
    public boolean supports(Update update) {
        if (!update.hasMessage()) {
            return false;
        }
        var msg = update.getMessage();
        if (msg.hasLocation()) return false;
        if (!msg.hasText()) return false;
        return GeoUtil.parseLatLon(msg.getText().trim(), new double[2]);
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        var text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        double[] coords = new double[2];

        if (!GeoUtil.parseLatLon(text, coords)) {
            // не забыть убрать
            return List.of(TelegramMessageFactory.createHintMessage(chatId));
        }

        pageLoader.loadAndSendPage(chatId, coords[0], coords[1], 0);
        return List.of();
    }
}