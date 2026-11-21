package giis.socialnetwork.e2e.functional.dto;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;

/** Base of the API response DTOs; prints as JSON so assertion failures show the server's data. */
public abstract class ApiDto {

    private static final Gson GSON = new Gson();

    protected static <T> List<T> orEmpty(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }

    @Override
    public String toString() {
        return GSON.toJson(this);
    }
}