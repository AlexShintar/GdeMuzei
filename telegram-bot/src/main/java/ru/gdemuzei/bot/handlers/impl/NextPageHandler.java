package ru.gdemuzei.bot.handlers.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gdemuzei.bot.handlers.UpdateHandler;
import ru.gdemuzei.bot.service.MuseumPageLoader;

import java.util.List;

@Component
@Order(60)
@RequiredArgsConstructor
public class NextPageHandler implements UpdateHandler {

    private final MuseumPageLoader pageLoader;

    @Override
    public boolean supports(Update update) {
        return update.hasCallbackQuery()
                && update.getCallbackQuery().getData().startsWith("next:");
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        var query = update.getCallbackQuery();
        var data = query.getData();
        long chatId = query.getMessage().getChatId();

        String payload = data.substring("next:".length());
        String[] parts = payload.split(":");
        String[] coord = parts[0].split(",");

        double lat = Double.parseDouble(coord[0]);
        double lon = Double.parseDouble(coord[1]);
        int offset = Integer.parseInt(parts[1]);

        pageLoader.loadAndSendPage(chatId, lat, lon, offset);

        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(query.getId())
                .build();

        return List.of(answer);
    }
}
