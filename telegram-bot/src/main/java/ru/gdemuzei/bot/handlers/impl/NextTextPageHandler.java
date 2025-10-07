package ru.gdemuzei.bot.handlers.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.gdemuzei.bot.handlers.UpdateHandler;
import ru.gdemuzei.bot.service.MuseumPageLoader;

import java.util.List;

@Component
@Order(50)
@RequiredArgsConstructor
public class NextTextPageHandler implements UpdateHandler {

    private final MuseumPageLoader pageLoader;

    @Override
    public boolean supports(Update update) {
        return update.hasCallbackQuery()
                && update.getCallbackQuery().getData().startsWith("nextText:");
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {  // временное решение, до внедрения возврата searchId из redis в callback_data
        var cb = update.getCallbackQuery();
        int offset = Integer.parseInt(cb.getData().substring("nextText:".length()));
        Message m = (Message) cb.getMessage();
        long chatId = m.getChatId();
        String msgText = m.getText();
        String query = extractQuery(msgText);
        pageLoader.loadAndSendTextQuery(chatId, query, offset);

        return List.of(AnswerCallbackQuery.builder()
                .callbackQueryId(cb.getId())
                .build());
    }

    private String extractQuery(String text) {
        final String marker = "Поиск по запросу: ";
        int idx = text.lastIndexOf(marker);
        return (idx >= 0) ? text.substring(idx + marker.length()).trim() : "";
    }
}
