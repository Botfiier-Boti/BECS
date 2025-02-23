package com.botifier.becs.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Botifier
 * 
 * AI assisted with Claude AI
 */
public class ObjectConfig implements IConfig {
	/**
	 * Map of config values
	 */
    private final Map<String, ConfigValue<?>> values = new ConcurrentHashMap<>();

    /**
     * Gson builder for ObjectConfig
     */
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(ConfigValue.class, new ConfigValueTypeAdapter())
        .create();

    /**
     * Puts value at the key
     * @param \<T\> T Data type
     * @param key String Key to use, case-insensitive
     * @param value T Value to place
     */
    public <T> void put(String key, @Nonnull T value) {
        values.put(normalizeKey(key), new ConfigValue<>(value, value.getClass()));
    }

    /**
     * Puts value at the key if it is absent
     * @param \<T\> T Data type 
     * @param key String Key to use, case-insensitive
     * @param value T Value to place
     */
    public <T> void putIfAbsent(String key, @Nonnull T value) {
        values.putIfAbsent(normalizeKey(key), new ConfigValue<>(value, value.getClass()));
    }

    /**
     * Gets a value from the map and casts it as Class\<T\>
     * @param \<T\> T Type of class
     * @param key String Key to pull from
     * @param type Class\<T\> Class to cast to 
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, Class<T> type) {
        ConfigValue<?> configValue = values.get(normalizeKey(key));
        if (configValue == null) {
            return null;
        }
        
        Object value = configValue.getValue();
        
        //Value is an instance of type, no other checks needed
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        
        // Handle primitive types
        if (type.isPrimitive()) {
            Class<?> boxedType = getBoxedType(type);
            if (boxedType != null) {
                Object result = getValue(key, boxedType);
                return (T) result;
            }
        }
        
        // Handle primitive type conversions if needed
        if (Number.class.isAssignableFrom(configValue.getType()) && Number.class.isAssignableFrom(type)) {
            Number num = (Number) value;
            return (T) convertNumber(num, type);
        }
        
        throw new ClassCastException(
            String.format("Cannot cast value of type %s to %s", 
                configValue.getType().getName(), type.getName())
        );
    }

    /**
     * Gets value assigned to specified key as a boolean
     * @param key String Key to use, case-insensitive
     * @return boolean 
     */
    public boolean getBoolean(String key) {
        Boolean value = getValue(key, Boolean.class);
        if (value == null) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return value;
    }

    /**
     * Gets value assigned to specified key as a boolean, otherwise default
     * @param key String Key to use, case-insensitive
     * @param defaultValue boolean Value to default to
     * @return boolean
     */
    public boolean getBooleanOrDefault(String key, boolean defaultValue) {
        Boolean value = getValue(key, Boolean.class);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Gets value assigned to specified key as an integer
     * @param key String Key to use, case-insensitive
     * @return int
     */
    public int getInteger(String key) {
        Integer value = getValue(key, Integer.class);
        if (value == null) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return value;
    }

    /**
     * Gets value assigned to specified key as an integer, otherwise default
     * @param key String Key to use, case-insensitive
     * @param defaultValue int Value to default to
     * @return int
     */
    public int getIntegerOrDefault(String key, int defaultValue) {
        Integer value = getValue(key, Integer.class);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets value assigned to specified key as a long
     * @param key String Key to use, case-insensitive
     * @return long
     */
    public long getLong(String key) {
        Long value = getValue(key, Long.class);
        if (value == null) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return value;
    }

    /**
     * Gets value assigned to specified key as a long, otherwise default
     * @param key String Key to use, case-insensitive
     * @param defaultValue long Value to default to
     * @return long
     */
    public long getLongOrDefault(String key, long defaultValue) {
        Long value = getValue(key, Long.class);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets value assigned to specified key as a float
     * @param key String Key to use, case-insensitive
     * @return float
     */
    public float getFloat(String key) {
        Float value = getValue(key, Float.class);
        if (value == null) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return value;
    }

    /**
     * Gets value assigned to specified key as a float, otherwise default
     * @param key String Key to use, case-insensitive
     * @param defaultValue float Value to default to
     * @return float
     */
    public float getFloatOrDefault(String key, float defaultValue) {
        Float value = getValue(key, Float.class);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets value assigned to specified key as a double
     * @param key String Key to use, case-insensitive
     * @return double
     */
    public double getDouble(String key) {
        Double value = getValue(key, Double.class);
        if (value == null) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return value;
    }

    /**
     * Gets value assigned to specified key as a double, otherwise default
     * @param key String Key to use, case-insensitive
     * @param defaultValue double Value to default to
     * @return double
     */
    public double getDoubleOrDefault(String key, double defaultValue) {
        Double value = getValue(key, Double.class);
        return value != null ? value : defaultValue;
    }

    /**
     * Normalizes the specified key for the map
     * @param key String Key to normalize
     * @return String Normalized key
     */
    private String normalizeKey(String key) {
        return key.trim().toLowerCase();
    }

    /**
     * Converts Number objects to the specified type
     * @param number Number Object to convert
     * @param targetType Class\<?\> Class to convert to 
     * @return Object Converted class
     */
    private Object convertNumber(Number number, Class<?> targetType) {
        if (targetType == Integer.class) return number.intValue();
        if (targetType == Long.class) return number.longValue();
        if (targetType == Float.class) return number.floatValue();
        if (targetType == Double.class) return number.doubleValue();
        if (targetType == Byte.class) return number.byteValue();
        if (targetType == Short.class) return number.shortValue();
        throw new IllegalArgumentException("Unsupported number type: " + targetType);
    }
    
    /**
     * Converts primitive classes to non-primitive versions
     * @param primitiveType Class\<?\> The primitive class
     * @return Class\<?\> The non-primitive version
     */
    private Class<?> getBoxedType(Class<?> primitiveType) {
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == short.class) return Short.class;
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == double.class) return Double.class;
        return null;
    }

    @Override
    public ObjectConfig readFile(String file) {
        try {
            return loadFromFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config file: " + file, e);
        }
    }

    @Override
    public ObjectConfig readFileOrDefault(String file, IConfig defaultConfig) {
        if (!(defaultConfig instanceof ObjectConfig)) {
            throw new IllegalArgumentException(
                "Config of type " + defaultConfig.getClass().getName() + 
                " is not valid for ObjectConfig reading."
            );
        }

        try {
            return loadFromFile(file);
        } catch (IOException e) {
            return (ObjectConfig) defaultConfig;
        }
    }

    @Override
    public void writeFile(String file) {
        try {
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(values, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file: " + file, e);
        }
    }

    /**
     * Loads an ObjectConfig from the specified file
     * @param file String Filepath of the file
     * @return ObjectConfig Loaded config
     * @throws IOException
     */
    private ObjectConfig loadFromFile(String file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            Type mapType = new TypeToken<ConcurrentHashMap<String, ConfigValue<?>>>(){}.getType();
            Map<String, ConfigValue<?>> loadedValues = gson.fromJson(reader, mapType);
            values.clear();
            values.putAll(loadedValues);
            return this;
        }
    }

    /**
     * Tries to load an ObjectConfig from a file, if it fails creates a new one
     * @param file String Filepath of the file
     * @return ObjectConfig
     */
    public static ObjectConfig loadOrCreateFromFile(String file) {
        ObjectConfig config = new ObjectConfig();
        return config.readFileOrDefault(file, config);
    }

    /**
     * Checks if the specified key is in the map
     * @param key String Key to check
     * @return boolean
     */
    public boolean contains(String key) {
        return values.containsKey(normalizeKey(key));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("-=Config Values=-\n");
        for (Map.Entry<String, ConfigValue<?>> entry : values.entrySet()) {
            ConfigValue<?> value = entry.getValue();
            sb.append(String.format("%s : (%s) %s\n",
                entry.getKey(),
                value.getType().getSimpleName(),
                value.getValue()));
        }
        return sb.toString();
    }
    
    /**
     * GSON Type adapter for ConfigValues
     */
    private static class ConfigValueTypeAdapter implements JsonSerializer<ConfigValue<?>>, JsonDeserializer<ConfigValue<?>> {
        @Override
        public JsonElement serialize(ConfigValue<?> src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("type", src.getType().getName());
            json.add("value", context.serialize(src.getValue()));
            return json;
        }

        @Override
        public ConfigValue<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();
                Class<?> type = Class.forName(jsonObject.get("type").getAsString());
                Object value = context.deserialize(jsonObject.get("value"), type);
                return new ConfigValue<>(value, type);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException("Failed to deserialize ConfigValue", e);
            }
        }
    }

    /**
     * ConfigValue class
     * @param <T> T Stored data type
     */
    private static class ConfigValue<T> {
        private final T value;
        private final Class<T> type;

        @SuppressWarnings("unchecked")
        public ConfigValue(T value, Class<?> type) {
            this.value = value;
            this.type = (Class<T>) type;
        }

        public T getValue() {
            return value;
        }

        public Class<?> getType() {
            return type;
        }
    }
}