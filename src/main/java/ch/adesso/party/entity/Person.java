package ch.adesso.party.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@ToString
public class Person {

    private String id;
    private String firstname;
    private String lastname;
    private String status;

    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime birthdate;

    public Person() {}

    public Person applyEvent(PersonCreatedEvent event) {
        this.id = event.getId();
        this.lastname = "testLn";
        this.firstname = "testFn";
        return this;
    }


    public JsonObject toJson(){
        return Json.createObjectBuilder()
                .add("id", id)
                .add("firstname", firstname)
                .add("lastname", lastname)
                .add("status", status)
                .add("birthdate", birthdate.toString())
                .build();
    }

}
