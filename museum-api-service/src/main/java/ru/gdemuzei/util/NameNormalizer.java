package ru.gdemuzei.util;

import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Нормализация названий музеев:
 * удаление диакритики, приведение к нижнему регистру, схлопывание пробелов.
 */
@NoArgsConstructor
public final class NameNormalizer {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");

    public static String normalize(String name) {
        if (name == null) {
            return null;
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase();
        normalized = normalized.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }
}
