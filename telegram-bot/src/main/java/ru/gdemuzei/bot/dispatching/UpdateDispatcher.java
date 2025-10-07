package ru.gdemuzei.bot.dispatching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gdemuzei.bot.handlers.UpdateHandler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateDispatcher {

    private final List<UpdateHandler> handlers;

    public List<BotApiMethod<?>> dispatch(Update update) {
        System.out.println(update);
        List<BotApiMethod<?>> out = new ArrayList<>();
        for (UpdateHandler h : handlers) {
            try {
                if (h.supports(update)) {
                    out.addAll(h.handle(update));
                    break;
                }
            } catch (Exception e) {
                log.warn("Handler {} failed: {}", h.getClass().getSimpleName(), e.toString());
            }
        }
        return out;
    }
}