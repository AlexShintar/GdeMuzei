package ru.gdemuzei.bot.repositories;

import org.springframework.stereotype.Component;
import ru.gdemuzei.bot.models.ConversationState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationStateStore {

    private final Map<Long, ConversationState> byChat = new ConcurrentHashMap<>();

    public ConversationState get(long chatId) {
        return byChat.getOrDefault(chatId, ConversationState.IDLE);
    }

    public void set(long chatId, ConversationState state) {
        byChat.put(chatId, state);
    }
}
