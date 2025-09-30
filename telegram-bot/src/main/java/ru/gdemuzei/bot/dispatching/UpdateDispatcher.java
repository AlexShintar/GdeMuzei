package ru.gdemuzei.bot.dispatching;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gdemuzei.bot.handlers.UpdateHandler;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UpdateDispatcher {

    private final List<UpdateHandler> handlers;

    public List<BotApiMethod<?>> dispatch(Update update) {
        List<BotApiMethod<?>> out = new ArrayList<>();
        for (UpdateHandler h : handlers) {
            if (h.supports(update)) {
                out.addAll(h.handle(update));
                break;
            }
        }
        return out;
    }
}
