package com.velopayments.blockchain.sdk.metadata;

public final class CamelCase {

    private CamelCase() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String upperUnderscoreToUpperCamelCase(String upperCaseName) {
        return transform(upperCaseName, false);
    }

    public static String upperUnderscoreToLowerCamelCase(String upperCaseName) {
        return transform(upperCaseName, true);
    }

    private static String transform(String upperCaseName, boolean firstCharacterLowerCase) {
        if (upperCaseName == null) {
            return null;
        }
        if (upperCaseName.length() == 0) {
            return "";
        }
        if (upperCaseName.length() == 1) {
            return upperCaseName.toLowerCase();
        }
        return toCamelCase(upperCaseName, firstCharacterLowerCase);
    }

    static String toCamelCase(String s, boolean firstCharacterLowerCase){
        String[] parts = s.split("_");
        StringBuilder camelCaseString = new StringBuilder();
        for (String part : parts){
            camelCaseString.append(toProperCase(part));
        }

        if (firstCharacterLowerCase) {
            String firstChar = Character.toString(camelCaseString.charAt(0)).toLowerCase();
            camelCaseString.setCharAt(0, firstChar.charAt(0));
        }

        return camelCaseString.toString();
    }

    static String toProperCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
