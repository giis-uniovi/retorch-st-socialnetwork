package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Gson that reads any non-array as an empty list, since the gateway's Lua encoder serialises empty arrays as {@code {}}. */
public final class ApiJson {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(List.class, new LenientListDeserializer())
            .create();

    private ApiJson() {}

    public static <T> List<T> parseList(JsonArray array, Class<T> type) {
        List<T> items = new ArrayList<T>();
        for (JsonElement element : array) {
            items.add(GSON.fromJson(element, type));
        }
        return items;
    }

    private static final class LenientListDeserializer implements JsonDeserializer<List<?>> {
        @Override
        public List<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            List<Object> list = new ArrayList<Object>();
            if (!json.isJsonArray()) {
                return list;
            }
            Type elementType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
            for (JsonElement element : json.getAsJsonArray()) {
                list.add(context.deserialize(element, elementType));
            }
            return list;
        }
    }
}
