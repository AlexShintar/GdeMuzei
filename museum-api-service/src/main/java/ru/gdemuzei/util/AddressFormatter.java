package ru.gdemuzei.util;

import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Формирует адрес из полей ответа Nominatim.
 */
@NoArgsConstructor
public final class AddressFormatter {

    /**
     * Собирает адрес вида: «улица, дом, город/посёлок, регион».
     */
    public static String buildAddress(Map<String, String> address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        String street = coalesce(address,
                "road", "pedestrian", "footway", "street", "residential", "living_street");
        String houseNumber = trimToNull(address.get("house_number"));
        String cityLike = coalesce(address, "city", "town", "village", "hamlet", "locality", "municipality");
        String region = coalesce(address, "state", "region", "province");
        List<String> parts = new ArrayList<>();

        if (!isBlank(street)) {
            parts.add(!isBlank(houseNumber) ? street + ", " + houseNumber : street);
        } else if (!isBlank(houseNumber)) {
            parts.add(houseNumber);
        }
        if (!isBlank(cityLike)) {
            parts.add(cityLike);
        }
        if (!isBlank(region)) {
            parts.add(region);
        }
        return String.join(", ", parts.stream()
                .map(AddressFormatter::normalize)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Возвращает «населённый пункт» из набора типовых ключей address.
     */
    public static String chooseLocality(Map<String, String> address) {
        if (address == null || address.isEmpty()) return null;
        return coalesce(address, "city", "town", "village", "hamlet", "municipality", "locality");
    }

    private static String coalesce(Map<String, String> map, String... keys) {
        for (String k : keys) {
            String v = trimToNull(map.get(k));
            if (v != null) return v;
        }
        return null;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}