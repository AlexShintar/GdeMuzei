package ru.gdemuzei.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import ru.gdemuzei.dto.MuseumCreateRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MuseumCsvParser {

    private static final Pattern TELEGRAM_PATTERN = Pattern.compile("t\\.me/(\\w+)");

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s()\\[\\]{}]+");

    private static final String IGNORED_DOMAIN = "xn--r1a.website";

    /**
     * Преобразует CSV-файл в список заявок на создание музеев.
     *
     * @param path путь к CSV-файлу в кодировке UTF-8
     * @return список DTO для создания музеев; при ошибке чтения — пустой список
     */
    public List<MuseumCreateRequest> parseMuseums(Path path) {
        List<MuseumCreateRequest> requests = new ArrayList<>();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(',')
                .setQuote('"')
                .setTrim(true).get();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVParser.parse(reader, csvFormat)) {

            for (CSVRecord csvRecord : csvParser) {
                if (csvRecord.size() < 4) {
                    log.warn("Skipping malformed CSV record: not enough columns. Record: {}", csvRecord);
                    continue;
                }
                try {
                    String name = csvRecord.get(0);
                    String urlsBlock = csvRecord.get(1);
                    double latitude = Double.parseDouble(csvRecord.get(2));
                    double longitude = Double.parseDouble(csvRecord.get(3));

                    String telegramUser = parseTelegramUser(urlsBlock);
                    String websiteUrl = parseWebsiteUrl(urlsBlock);

                    requests.add(new MuseumCreateRequest(name, longitude, latitude, telegramUser, websiteUrl));
                } catch (Exception e) {
                    log.error("Failed to parse CSV record: {}", csvRecord, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read or parse CSV file: {}", path.getFileName(), e);
            return Collections.emptyList();
        }
        return requests;
    }

    /**
     * Извлекает адрес Telegram из блока текста.
     * Находит первое совпадение и возвращает его.
     * @param urlsBlock Текстовый блок со ссылками.
     * @return Telegram или null, если не найден.
     */
    private String parseTelegramUser(String urlsBlock) {
        if (urlsBlock == null || urlsBlock.isBlank()) {
            return null;
        }
        Matcher matcher = TELEGRAM_PATTERN.matcher(urlsBlock);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Извлекает основной URL веб-сайта из блока текста.
     * Игнорирует ссылки на Telegram и нежелательные домены.
     * @param urlsBlock Текстовый блок со ссылками.
     * @return URL сайта или null, если не найден подходящий.
     */
    private String parseWebsiteUrl(String urlsBlock) {
        if (urlsBlock == null || urlsBlock.isBlank()) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(urlsBlock);

        List<String> allUrls = new ArrayList<>();
        while (matcher.find()) {
            allUrls.add(matcher.group());
        }

        return allUrls.stream()
                .filter(Objects::nonNull)
                .filter(url -> !url.contains("t.me"))
                .filter(url -> !url.contains(IGNORED_DOMAIN))
                .findFirst()
                .orElse(null);
    }
}