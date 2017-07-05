package ch.adesso.party.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.avro.reflect.AvroDefault;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Person {

    @AvroDefault("0")
    private long version = 0;

    private String partyId;
    @AvroDefault("null")
    private String firstname;
    @AvroDefault("null")
    private String lastname;

    public Person applyEvent(PartyEvent event) {
        System.out.println("got event PartyEvent !!!!! " + event);
        if(event instanceof PersonCreatedEvent) {
            applyEvent((PersonCreatedEvent)event);
        } else if((event instanceof PersonChangedEvent)) {
            applyEvent((PersonChangedEvent) event);
        }
        return this;
    }

    public Person applyEvent(PersonChangedEvent event) {
        System.out.println("got event PersonChangedEvent !!!!! " + event);
        firstname = event.getFirstname();
        lastname = event.getLastname();
        version = version + 1;
        return this;
    }

    public Person applyEvent(PersonCreatedEvent event) {
        System.out.println("got event PersonCreatedEvent !!!!! " + event);
        firstname = event.getFirstname();
        lastname = event.getLastname();
        partyId = event.getPartyId();
        return this;
    }

}
