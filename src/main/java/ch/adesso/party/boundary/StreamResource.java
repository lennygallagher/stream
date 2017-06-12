package ch.adesso.party.boundary;

import ch.adesso.party.entity.Person;
import ch.adesso.party.kafka.KafkaStreamProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by tom on 12.06.17.
 */
@Path("streams")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
public class StreamResource {

    @Inject
    private KafkaStreams kafkaStreams;

    @GET
    @Path("/test")
    public String test() {
        return "hello";
    }

    @GET
    public Person[] all() {
        try {
            ReadOnlyKeyValueStore<String, Person> store = KafkaStreamProvider.waitUntilStoreIsQueryable(KafkaStreamProvider.PERSON_STORE,
                    QueryableStoreTypes.<String, Person>keyValueStore(), kafkaStreams);
            KeyValueIterator<String, Person> it = store.all();

            List<Person> persons = new ArrayList<>();
            while (it.hasNext()) {
                KeyValue<String, Person> kv = it.next();
                persons.add(kv.value);
            }

            return persons.toArray(new Person[]{});

        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @GET
    @Path("/person/{id}")
    public Person person(@PathParam("id") String personId) {

        try {
            ReadOnlyKeyValueStore<String, Person> store = KafkaStreamProvider.waitUntilStoreIsQueryable(KafkaStreamProvider.PERSON_STORE,
                    QueryableStoreTypes.<String, Person>keyValueStore(), kafkaStreams);

            final Person person = store.get(personId);
            if (person == null) {
                throw new NotFoundException(String.format("Person with id [%s] was not found", personId));
            }

            return person;

        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

}
