package space.jamestang.simpletimer.client.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.jamestang.simpletimer.client.network.Payload;

public class Serializer {

    private final Logger logger = LoggerFactory.getLogger(Serializer.class);

    /**
     * Singleton instance of Serializer.
     * This class is designed to be a utility class for serialization tasks.
     */
    public static final Serializer INSTANCE = new Serializer();
    private final ObjectMapper mapper;

    private Serializer() {
        // Private constructor to prevent instantiation
        this.mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
    }

    public byte[] toBytes(Object obj){
        try {
            var clazz = obj.getClass().getName();
            return mapper.writeValueAsBytes(new Payload(clazz, obj));
        } catch (Exception e) {
            logger.error("Serialization failed for object: {}", obj, e);
            throw new RuntimeException("Serialization failed", e);
        }
    }

    public <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        try {
            var payload = mapper.readValue(bytes, Payload.class);
            if (!payload.clazzInfo().equals(clazz.getName())) {
                throw new IllegalArgumentException("Class mismatch: expected " + clazz.getName() + ", but got " + payload.clazzInfo());
            }
            return mapper.convertValue(payload.content(), clazz);
        } catch (Exception e) {
            logger.error("Deserialization failed for bytes: {}", bytes, e);
            throw new RuntimeException("Deserialization failed", e);
        }
    }
}
