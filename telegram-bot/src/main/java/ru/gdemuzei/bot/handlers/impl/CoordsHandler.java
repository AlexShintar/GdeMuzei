package ru.gdemuzei.bot.handlers.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.location.Location;
import ru.gdemuzei.bot.handlers.UpdateHandler;
import ru.gdemuzei.bot.service.MuseumPageLoader;
import ru.gdemuzei.bot.telegram.TelegramMessageFactory;
import ru.gdemuzei.bot.util.GeoUtil;

import java.util.List;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class CoordsHandler implements UpdateHandler {

    private final MuseumPageLoader pageLoader;

    @Override
    public boolean supports(Update update) {
        if (!update.hasMessage()) {
            return false;
        }
        var msg = update.getMessage();
        return msg.hasLocation() || (msg.hasText() && !"/start".equals(msg.getText()));
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        var message = update.getMessage();
        long chatId = message.getChatId();

        double[] coords = new double[2];
        boolean parsed = message.hasLocation()
                ? parseFromLocation(message.getLocation(), coords)
                : parseFromText(message.getText(), coords);

        if (!parsed) {
            return List.of(TelegramMessageFactory.createHintMessage(chatId));
        }

        pageLoader.loadAndSendPage(chatId, coords[0], coords[1], 0);
        return List.of();
    }

    private boolean parseFromLocation(Location loc, double[] out) {
        out[0] = loc.getLatitude();
        out[1] = loc.getLongitude();
        return true;
    }

    private boolean parseFromText(String text, double[] out) {
        return GeoUtil.parseLatLon(text, out);
    }
}
