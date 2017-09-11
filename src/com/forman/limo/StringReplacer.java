package com.forman.limo;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringReplacer {
    private static String REGEX = "(\\{[^}]+\\})";
    private static Pattern PATTERN = Pattern.compile(REGEX);
    private static Map<String, Integer> INDEX_NAMES = new HashMap<String, Integer>() {{
        for (int i = 1; i <= 100; i++) {
            put(String.format("{%0" + i + "dN}", 0), i + 1);
        }
    }};

    public static String replace(String pattern, int index, Map<String, String> replacements) {
        StringBuffer sb = new StringBuffer();
        Matcher m = PATTERN.matcher(pattern);
        while (m.find()) {
            String group = m.group(1);
            if (group != null) {
                String replacement = null;
                if (group.equalsIgnoreCase("{N}")) {
                    replacement = Integer.toString(index);
                } else if (INDEX_NAMES.containsKey(group)) {
                    int n = INDEX_NAMES.get(group);
                    replacement = String.format("%0" + n + "d", index);
                } else {
                    replacement = replacements.get(group);
                }
                if (replacement != null) {
                    m.appendReplacement(sb, replacement);
                }
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
