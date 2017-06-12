package ch.adesso.party.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * Created by tom on 11.06.17.
 */
@Data
@AllArgsConstructor
@ToString
public class CoreEvent {
    private String id;
    private String name;

    public CoreEvent () {}

    public void fromJsonObject(JsonObject jsonObject) {
        String name = jsonObject.getString("name");
        String id = jsonObject.getString("id");

        this.id = id;
        this.name = name;
    }

    public JsonObject toJson(){
        return Json.createObjectBuilder()
                .add("id", id)
                .add("name", name)
                .build();
    }

}
