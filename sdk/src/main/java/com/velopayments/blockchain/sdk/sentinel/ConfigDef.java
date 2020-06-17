package com.velopayments.blockchain.sdk.sentinel;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Specifies a set of configurations. For each configuration has a name, type, the default value.
 */
public class ConfigDef {
    private static final Pattern COMMA_WITH_WHITESPACE_PATTERN = Pattern.compile("\\s*,\\s*");

    private final Map<String, ConfigKey> keys;

    ConfigDef(Map<String, ConfigKey> keys) {
        this.keys = keys;
    }

    public static ConfigDefBuilder builder() {
        return new ConfigDefBuilder();
    }

    /**
     * Create a map of settings from give intput properties
     */
    public Map<String, Object> mapToSettings(String name, Properties properties) {
        Map<String, Object> settings = new HashMap<>();
        for(Map.Entry<String, ConfigKey> keyDef : keys.entrySet()) {
            String propName = String.format("sentinel.%s.%s", name, keyDef.getKey());
            Object value = Optional.ofNullable(properties.getProperty(propName))
                .map(v -> parseType(keyDef.getKey(), v, keyDef.getValue().type))
                .or(() -> Optional.ofNullable(keyDef.getValue().defaultValue))
                .orElseThrow(() -> new SentinelConfigException("Missing required configuration property: " + propName));
            settings.put(keyDef.getKey(), value);
        }
        return settings;
    }

    private Object parseType(String name, String value, Type type) {
        String trimmed = value.trim();
        switch (type) {
            case STRING:
                return trimmed;
            case BOOLEAN:
                if (trimmed.equalsIgnoreCase("true"))
                    return true;
                else if (trimmed.equalsIgnoreCase("false"))
                    return false;
                else
                    throw new SentinelConfigException(name, value, "Expected value to be either true or false");
            case INT:
                try {
                    return Integer.parseInt(trimmed);
                } catch (NumberFormatException ex) {
                    throw new SentinelConfigException(name, value, "Expected value to be a 32-bit integer");
                }
            case SHORT:
                try {
                    return Short.parseShort(trimmed);
                } catch (NumberFormatException ex) {
                    throw new SentinelConfigException(name, value, "Expected value to be a 16-bit integer (short)");
                }
            case LONG:
                try {
                    return Long.parseLong(trimmed);
                } catch (NumberFormatException ex) {
                    throw new SentinelConfigException(name, value, "Expected value to be a 64-bit integer (long)");
                }
            case DOUBLE:
                try {
                    return Double.parseDouble(trimmed);
                } catch (NumberFormatException ex) {
                    throw new SentinelConfigException(name, value, "Expected value to be a double.");
                }
            case LIST:
                if (trimmed.isEmpty())
                    return Collections.emptyList();
                else
                    return Arrays.asList(COMMA_WITH_WHITESPACE_PATTERN.split(trimmed, -1));
            case CLASS:
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                try {
                    Class<?> klass = cl.loadClass(trimmed);
                    return Class.forName(klass.getName(), true, cl);
                } catch (ClassNotFoundException ex) {
                    throw new SentinelConfigException(name, value, "Class " + value + " could not be found.");
                }
            default:
                throw new IllegalStateException("Unknown type.");
        }
    }

    @AllArgsConstructor
    public static class ConfigKey {
        @NonNull
        public final Type type;

        public final Object defaultValue;
    }

    public enum Type {
        BOOLEAN, STRING, INT, SHORT, LONG, DOUBLE, LIST, CLASS // TODO: password/or maybe cert?
    }

    public static class ConfigDefBuilder {
        private ArrayList<String> keys$key;
        private ArrayList<ConfigKey> keys$value;
        private @NonNull String name;

        public ConfigDefBuilder key(String key, Type type) {
            return key(key, type, null);
        }

        public ConfigDefBuilder key(String key, Type type, Object defaultValue) {
            if (key.contains(" ")) throw new IllegalArgumentException(String.format("Configuration keys must not contain spaces: \"%s\"", key));
            if (this.keys$key == null) {
                this.keys$key = new ArrayList<>();
                this.keys$value = new ArrayList<>();
            }
            this.keys$key.add(key);
            this.keys$value.add(new ConfigKey(type, defaultValue));
            return this;
        }

        public ConfigDefBuilder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        public ConfigDef build() {
            Map<String, ConfigKey> keys;
            switch (this.keys$key == null ? 0 : this.keys$key.size()) {
                case 0:
                    keys = Collections.emptyMap();
                    break;
                case 1:
                    keys = Collections.singletonMap(this.keys$key.get(0), this.keys$value.get(0));
                    break;
                default:
                    keys = new LinkedHashMap<>(this.keys$key.size() < 1073741824 ? 1 + this.keys$key.size() + (this.keys$key.size() - 3) / 3 : Integer.MAX_VALUE);
                    for (int $i = 0; $i < this.keys$key.size(); $i++)
                        keys.put(this.keys$key.get($i), this.keys$value.get($i));
                    keys = Collections.unmodifiableMap(keys);
            }

            return new ConfigDef(keys);
        }
    }
}
