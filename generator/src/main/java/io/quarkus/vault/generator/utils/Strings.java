package io.quarkus.vault.generator.utils;

public class Strings {
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String decapitalize(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    public static String kebabCaseToSnakeCase(String s) {
        return s.replace('-', '_');
    }

    public static String camelCaseToSnakeCase(String s) {
        var sb = new StringBuilder();
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("_");
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String camelCaseToTitle(String s) {
        var sb = new StringBuilder();
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(" ");
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        return capitalize(sb.toString());
    }
}
