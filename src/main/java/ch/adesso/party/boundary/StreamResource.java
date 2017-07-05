package ch.adesso.party.boundary;

import ch.adesso.party.entity.PartyEvents;
import ch.adesso.party.entity.Person;
import ch.adesso.party.entity.TestAvro;
import ch.adesso.party.kafka.KafkaStreamProvider;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
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
import java.util.List;
import java.util.Random;

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

    @Inject
    private KafkaProducer<String, Object> kafkaProducer;

    @POST
    public String testAvro(TestAvro avro) {
        System.out.println(avro);

        TestAvro t = TestAvro.newBuilder()
                .setId(""+new Random().nextInt())
                .setTimestamp(avro.getTimestamp())
                .setType(avro.getType())
                .build();
        kafkaProducer.send(new ProducerRecord<String, Object>("avro-test", t.getId(), t),
                (RecordMetadata recordMetadata, Exception e) -> {
                         if(e != null) e.printStackTrace();
                         else System.out.println(recordMetadata);}
        );

        System.out.println("Return: " + avro);

        return avro.toString();
    }

    @GET
    @Path("/test")
    public TestAvro test() {
        return new TestAvro();
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
