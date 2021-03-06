package ch.adesso.party.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.avro.reflect.AvroDefault;

/**
 * Created by hackathon on 11.06.17.
 */
@Data
@ToString
@NoArgsConstructor
public class PersonCreatedEvent extends PartyEvent {

    private String partyId;

    @AvroDefault("null")
    private String firstname;
    @AvroDefault("null")
    private String lastname;
    public PersonCreatedEvent(String partyId, String firstname, String lastname) {
        super(PersonCreatedEvent.class);
        this.partyId = partyId;
        this.firstname = firstname;
        this.lastname = lastname;
    }

}
