package ch.adesso.party.entity;

import javax.json.JsonObject;

/**
 * Created by hackathon on 11.06.17.
 */
public class PersonCreatedEvent extends CoreEvent {

    public static String NAME = "ch.adesso.partyservice.party.personcreated";

    public PersonCreatedEvent() {
        super();
    }

    public void fromJsonObject(JsonObject jsonObject) {
        super.fromJsonObject(jsonObject);
    }

}
