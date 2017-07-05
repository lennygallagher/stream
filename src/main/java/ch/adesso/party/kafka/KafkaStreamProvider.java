package ch.adesso.party.kafka;

import ch.adesso.party.entity.EventEnvelope;
import ch.adesso.party.entity.Person;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.state.QueryableStoreType;
import org.apache.kafka.streams.state.StreamsMetadata;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.enterprise.inject.Produces;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;


@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KafkaStreamProvider {

    public static final String PERSON_TOPIC = "person";
    public static final String PERSON_STORE = "person-store";
    public static final String PERSON_ADDRESS_TOPIC = "person-address-topic";
    public static final String ADDRESS_TOPIC = "address-topic";

    private static final String KAFKA_BROKER_HOST = System.getenv("KAFKA_BROKER_HOST") != null ? System.getenv("KAFKA_BROKER_HOST"): "localhost";
    private static final String BOOTSTRAP_SERVERS = "kafka-1:29092,kafka-2:39092,kafka-3:49092";
    private static final String SCHEMA_REGISTRY_URL = "http://schema-registry:8081";

    private static final String APPLICATION_CONFIG_ID = "streams-app";
    private static final String APPLICATION_SERVER_ID = "localhost:8080";
    private static final String STATE_DIR = "/tmp/kafka-streams";


    private KafkaStreams kafkaStreams;
    private KafkaProducer<String, Object> producer;

    @PostConstruct
    public void init() {
        this.producer = createProducer();
        this.kafkaStreams = createKafkaStreams();
    }

    @PreDestroy
    public void close() {
        this.kafkaStreams.close();
    }

    @Produces
    public KafkaProducer<String, Object> getProducer() {
        return producer;
    }

    @Produces
    public KafkaStreams getKafkaStreams() {
        return kafkaStreams;
    }


    public KafkaProducer<String, Object> createProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        properties.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        return new KafkaProducer<>(properties);
    }

    public KafkaStreams createKafkaStreams() {
        final Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_CONFIG_ID);
        // Where to find Kafka broker(s).
        System.out.println("BROKERS: " + BOOTSTRAP_SERVERS);
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        // Provide the details of our embedded http service that we'll use to connect to this streams
        // instance and discover locations of stores.
        streamsConfiguration.put(StreamsConfig.APPLICATION_SERVER_CONFIG, APPLICATION_SERVER_ID);
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, STATE_DIR);
        // Set to earliest so we don't miss any data that arrived in the topics before the process
        // started
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Set the commit interval to 500ms so that any changes are flushed frequently and the top five
        // charts are updated with low latency.
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);
        // Allow the user to fine-tune the `metadata.max.age.ms` via Java system properties from the CLI.
        // Lowering this parameter from its default of 5 minutes to a few seconds is helpful in
        // situations where the input topic was not pre-created before running the application because
        // the application will discover a newly created topic faster.  In production, you would
        // typically not change this parameter from its default.

        streamsConfiguration.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());

        String metadataMaxAgeMs = System.getProperty(ConsumerConfig.METADATA_MAX_AGE_CONFIG);
        if (metadataMaxAgeMs != null) {
            try {
                int value = Integer.parseInt(metadataMaxAgeMs);
                streamsConfiguration.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, value);
                System.out.println("Set consumer configuration " + ConsumerConfig.METADATA_MAX_AGE_CONFIG +
                        " to " + value);
            } catch (NumberFormatException ignored) {
            }
        } else
            streamsConfiguration.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 500);

        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);


        final Serde<EventEnvelope> eventSerde = Serdes.serdeFrom(new KafkaAvroReflectSerializer<>(),
                new KafkaAvroReflectDeserializer<>(EventEnvelope.class));

        // important to configure schema registry
        eventSerde.configure(
                Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        SCHEMA_REGISTRY_URL),false);

        final Serde<Person> personSerde = Serdes.serdeFrom(new KafkaAvroReflectSerializer<>(),
                new KafkaAvroReflectDeserializer<>(Person.class));

        personSerde.configure(
                Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        SCHEMA_REGISTRY_URL),false);

        final KStreamBuilder builder = new KStreamBuilder();

        // read person event stream
        KStream<String, EventEnvelope> personEventStream = builder.stream(Serdes.String(), eventSerde, PERSON_TOPIC);

        // aggregate events to person
        personEventStream.groupByKey(Serdes.String(), eventSerde)
                .aggregate(Person::new,
                        (aggKey, newValue, person) -> person.applyEvent(newValue.getEvent()),
                        personSerde,
                        PERSON_STORE);


        KafkaStreams streams = new KafkaStreams(builder, new StreamsConfig(streamsConfiguration));

        streams.cleanUp();
        streams.start();

        return streams;
    }

    public static <T> T waitUntilStoreIsQueryable(final String storeName,
                                                  final QueryableStoreType<T> queryableStoreType,
                                                  final KafkaStreams streams) throws InterruptedException {
        while (true) {
            try {
                return streams.store(storeName, queryableStoreType);
            } catch (InvalidStateStoreException ignored) {
                // store not yet ready for querying
                Collection<StreamsMetadata> hosts = streams.allMetadataForStore(storeName);
                hosts.forEach(
                        metaData -> System.out.println(metaData.host() + ":" + metaData.port())
                );
                Thread.sleep(100);
            }
        }
    }
}

