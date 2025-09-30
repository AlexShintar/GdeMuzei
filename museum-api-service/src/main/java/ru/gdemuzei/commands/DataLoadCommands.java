package ru.gdemuzei.commands;

import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import ru.gdemuzei.services.DataLoadService;

import java.nio.file.Path;

@ShellComponent
//@Profile("shell")
@RequiredArgsConstructor
@Slf4j
public class DataLoadCommands {

    private final DataLoadService dataLoadService;

    @Value("${data.import.museum-csv-path:museums.csv}")
    private String defaultCsvPath;


    /**
     * Загружает данные о музеях из CSV-файла.
     * <p>Если путь не указан, используется значение по умолчанию из конфигурации
     * {@code data.import.museum-csv-path}. В ответ возвращается итоговый текст
     * со статистикой загруженных записей или сообщением об ошибке.</p>
     *
     * @param filePath путь к CSV-файлу или {@code null} для использования значения по умолчанию
     * @return отчёт о результате загрузки
     */
    @ShellMethod(key = "load", value = "Load initial museum data from a CSV file.") //load-from-file
    public String loadMuseumsFromFile(@ShellOption(defaultValue = ShellOption.NULL) String filePath) {
        String path = (filePath == null || filePath.isBlank()) ? defaultCsvPath : filePath;
        try {
            int count = dataLoadService
                    .loadMuseumsFromCsv(Path.of(path))
                    .blockOptional()
                    .orElse(0);
            return "Successfully loaded " + count + " new museums.";
        } catch (Exception e) {
            return "An error occurred during file loading: " + e.getMessage();
        }
    }

    /**
     * Обогащает уже существующие записи музеев данными из Nominatim/OSM.
     * <p>Процесс останавливается, если обнаружен лимит/бан (HTTP 429/403).
     *
     * @return отчёт о завершении процесса
     */
    @ShellMethod(key = "osm", value = "Enrich existing museums with data from OSM.")//enrich-from-osm
    public String enrichFromOsm() {
        log.info("Starting enrichment process...");

        dataLoadService.enrichUnverifiedMuseumsFromOsm()
                .block(); // убрать?

        return "Enrichment process completed.";
    }
}
