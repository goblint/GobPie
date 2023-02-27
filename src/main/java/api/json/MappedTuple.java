package api.json;

public interface MappedTuple {

    /**
     * Returns fields which will be placed in/read from a tuple during serialization/deserialization.
     * Order of fields in the tuple will match the returned order.
     * For example returning {@code ["key", "value"]} will deserialize an object {@code obj} as the tuple {@code [obj.key, obj.value]}.
     */
    String[] getMappedFields();

}
