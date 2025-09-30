package ru.gdemuzei.bot.repositories;

import org.springframework.stereotype.Component;
import ru.gdemuzei.bot.dto.MuseumSummaryDto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class MuseumCache {
    private final Map<Long, Map<String, MuseumSummaryDto>> byChat = new ConcurrentHashMap<>();

    public void put(long chatId, MuseumSummaryDto dto) {

        byChat.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>())
                .put(dto.id(), dto);
    }

    public MuseumSummaryDto get(long chatId, String id) {
        var m = byChat.get(chatId);
        return m == null ? null : m.get(id);
    }
}
