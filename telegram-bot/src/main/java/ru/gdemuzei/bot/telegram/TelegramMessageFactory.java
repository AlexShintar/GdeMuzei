package ru.gdemuzei.bot.telegram;

import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVenue;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.gdemuzei.bot.dto.MuseumSummaryDto;
import ru.gdemuzei.bot.util.GeoUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TelegramMessageFactory {

    private static final String COORDINATES_INSTRUCTION =
            "Нажмите кнопку «📍 Отправить геолокацию»\nили введите свою широту и долготу, например: 59.9398,30.3146";

    public static SendMessage createListItemMessage(long chatId, MuseumSummaryDto dto) {
        String distance = GeoUtil.formatDistance(dto.distanceKm());
        String text = "<b>" + dto.name() + "</b>\n";
        String callbackData = "info:" + Objects.toString(dto.id(), "");

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .parseMode(ParseMode.HTML)
                .replyMarkup(createSingleButtonKeyboard("Расстояние - " + distance, callbackData))
                .build();
    }

    public static SendMessage createNextButtonMessage(long chatId, double lat, double lon, int nextOffset) {
        String callbackData = String.format(Locale.US, "next:%.6f,%.6f:%d", lat, lon, nextOffset);
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Показать ещё музеи?")
                .replyMarkup(createSingleButtonKeyboard("Дальше", callbackData))
                .build();
    }

    public static SendMessage createStartMessage(long chatId) {
        KeyboardButton locBtn = KeyboardButton.builder()
                .text("📍 Отправить геолокацию")
                .requestLocation(true)
                .build();
        KeyboardRow row = new KeyboardRow();
        row.add(locBtn);
        ReplyKeyboardMarkup replyKb = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .resizeKeyboard(true)
                .build();

        String text = "Привет! Я подскажу ближайшие музеи.\n\n" + COORDINATES_INSTRUCTION;

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(replyKb)
                .build();
    }

    public static SendMessage createHintMessage(long chatId) {
        String txt = "Неверный формат. " + COORDINATES_INSTRUCTION;
        return SendMessage.builder().chatId(String.valueOf(chatId)).text(txt).build();
    }

    public static SendMessage createErrorMessage(long chatId, String text) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();
    }

    private static InlineKeyboardMarkup createSingleButtonKeyboard(String text, String callbackData) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
        InlineKeyboardRow row = new InlineKeyboardRow(button);
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
    }

    public static SendMessage createInfoMessage(long chatId, MuseumSummaryDto dto) {
        List<String> lines = new ArrayList<>();
        lines.add("<b>" + dto.name() + "</b>");

        if (dto.address() != null && !dto.address().isBlank()) {
            lines.add("Адрес: " + dto.address());
        }
        if (dto.website() != null && !dto.website().isBlank()) {
            lines.add("Сайт: <a href=\"" + dto.website() + "\">" + dto.website() + "</a>");
        }
        if (dto.telegramChannel() != null && !dto.telegramChannel().isBlank()) {
            String channel = dto.telegramChannel();
            lines.add("Telegram: <a href=\"https://t.me/" + channel + "\">" + channel + "</a>");
        }
        String text = String.join("\n", lines);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .parseMode(ParseMode.HTML)
                .build();
    }

    public static SendVenue createVenueMessage(long chatId, MuseumSummaryDto dto) {
        return SendVenue.builder()
                .chatId(String.valueOf(chatId))
                .latitude(dto.lat())
                .longitude(dto.lon())
                .title(limit(dto.name(), 128))
                .address(limit(safeAddress(dto), 256))
                .build();
    }

    private static String safeAddress(MuseumSummaryDto dto) {
        if (dto.address() != null && !dto.address().isBlank()) {
            return dto.address();
        }
        return String.format(java.util.Locale.US, "%.6f, %.6f", dto.lat(), dto.lon());
    }

    private static String limit(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
