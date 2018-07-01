package mystdeim.nanobank.data

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@DataObject
class Account(var id: String, var person: String) {
    constructor(json: JsonObject) :this(json.getString("id"), json.getString("person")) {

    }
    fun toJson() : JsonObject {
        val json = JsonObject()
        json.put("id", id)
        json.put("person", person)
        return json
    }
}