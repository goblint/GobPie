package api.json;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A class for handling unexpected fields in GobPie configuration.
 * Code adapted from <a href="https://github.com/google/gson/issues/188">a Gson library issue</a>.
 *
 * @since 0.0.4
 */

public class GobPieConfValidatorAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        // If the type adapter is a reflective type adapter, we want to modify the implementation using reflection.
        // The trick is to replace the Map object used to look up the property name.
        // Instead of returning null if the property is not found,
        // we throw a Json exception to terminate the deserialization.
        TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, type);

        // Check if the type adapter is a reflective, cause this solution only work for reflection.
        if (delegateAdapter instanceof ReflectiveTypeAdapterFactory.Adapter) {

            try {
                // Get reference to the existing boundFields.
                Field f = findBoundField(delegateAdapter.getClass());
                f.setAccessible(true);
                // Finally, push our custom map back using reflection.
                f.set(delegateAdapter, getBoundFields(f, delegateAdapter));
            } catch (Exception e) {
                // Should never happen if the implementation doesn't change.
                throw new IllegalStateException(e);
            }

        }
        return delegateAdapter;
    }

    @SuppressWarnings("unchecked")
    private static <T> Object getBoundFields(Field f, TypeAdapter<T> delegate) throws IllegalAccessException {
        Object boundFields = f.get(delegate);

        // Then replace it with our implementation throwing exception if the value is null.
        boundFields = new LinkedHashMap<>((Map<Object, Object>) boundFields) {

            @Override
            public Object get(Object key) {
                Object value = super.get(key);
                if (value == null) {
                    throw new JsonParseException(String.valueOf(key));
                }
                return value;
            }

        };
        return boundFields;
    }

    private static Field findBoundField(Class<?> startingClass) throws NoSuchFieldException {
        for (Class<?> c = startingClass; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField("boundFields");
            } catch (NoSuchFieldException e) {
                // OK: continue with superclasses
            }
        }
        throw new NoSuchFieldException("boundFields starting from " + (startingClass != null ? startingClass.getName() : null));
    }
}
