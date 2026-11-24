package com.astrax.ai;

import java.util.regex.Pattern;

/**
 * Che thông tin nhạy cảm (email, IP, mật khẩu, token, số thẻ) trước khi gửi cho AI
 * và trước khi lưu vào bộ nhớ hội thoại.
 */
public final class Sanitizer {

    private static final String MASK = "[đã ẩn]";

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final Pattern PASSWORD = Pattern.compile(
            "\\b(?:mật khẩu|mat khau|password|passwd|pass|pwd)\\b[^\\n]{0,15}?(?:[:=]|\\blà\\b|\\bla\\b|\\bis\\b)\\s*\\S+",
            FLAGS | Pattern.UNICODE_CHARACTER_CLASS); // cần để \b hiểu chữ có dấu như "à"
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern IPV4 = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern API_KEY = Pattern.compile(
            "\\b(?:sk|pk|key|token)[-_][A-Za-z0-9_\\-]{16,}\\b", FLAGS);
    private static final Pattern LONG_TOKEN = Pattern.compile(
            "\\b[A-Za-z0-9_\\-]{32,}\\b");
    private static final Pattern CARD = Pattern.compile(
            "\\b(?:\\d[ -]?){13,19}\\b");

    private Sanitizer() {
    }

    public static String redact(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String s = input;
        s = PASSWORD.matcher(s).replaceAll(MASK);
        s = EMAIL.matcher(s).replaceAll(MASK);
        s = IPV4.matcher(s).replaceAll(MASK);
        s = API_KEY.matcher(s).replaceAll(MASK);
        s = LONG_TOKEN.matcher(s).replaceAll(MASK);
        s = CARD.matcher(s).replaceAll(MASK);
        return s;
    }
}
