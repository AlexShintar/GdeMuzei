package ru.gdemuzei.bot.repositories;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.gdemuzei.bot.dto.MuseumSummaryDto;

@Component
@RequiredArgsConstructor
public class MuseumCache {

    private final Cache<String, MuseumSummaryDto> cache;

    public void put(MuseumSummaryDto dto) {
        if (dto != null && dto.id() != null) cache.put(dto.id(), dto);
    }

    public MuseumSummaryDto get(String id) {
        return cache.getIfPresent(id);
    }

//    public void putAll(Collection<MuseumSummaryDto> dtos) {
//        dtos.forEach(this::put);
//    }
}