package app.dassana.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.micronaut.context.annotation.Factory;
import javax.inject.Singleton;
import org.json.JSONException;
import org.json.JSONObject;

@Factory
public class JsonyThings {

  public static final ObjectMapper MAPPER = new ObjectMapper();


  @Singleton
  Gson getGson() {
    return new Gson();
  }

  public static void throwExceptionIfNotValidJsonObj(String strToValidate) {

    try {
      JSONObject jsonObject = new JSONObject(strToValidate);
    } catch (JSONException e) {
      throw new IllegalArgumentException("Sorry, Dassana Engine can only process JSON objects");

    }

  }


}
