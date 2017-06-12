package ch.adesso.party.entity;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by tom on 12.06.17.
 */
public class EventConverter {

    private enum Events {
        PERSON_CREATED(PersonCreatedEvent.NAME, PersonCreatedEvent.class);

        private String eventType;
        private Class<? extends CoreEvent> cls;
        Events(String name, Class<? extends CoreEvent> cls) {
            this.eventType = name;
            this.cls = cls;
        }

        public static Class<? extends CoreEvent> loadClass(String eventType) {
            for(Events e : Events.values()) {
                if(e.eventType.equals(eventType)) {
                    return e.cls;
                }
            }
            return null;
        }
    }

    public static <T extends CoreEvent> T convert(String jsonAsString) {
        InputStream inputStream = new ByteArrayInputStream(jsonAsString.getBytes(Charset.forName("UTF-8")));
        JsonObject eventObject = Json.createReader(inputStream).readObject();
        String name = eventObject.getString("name");

        try {
            Class<? extends CoreEvent> cls = Events.loadClass(name);
            T evt = (T)cls.newInstance();
            evt.fromJsonObject(eventObject);
            return evt;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
