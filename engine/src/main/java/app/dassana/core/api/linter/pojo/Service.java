package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Service {
	@JsonProperty
	public String id;
	@JsonProperty("resource-types")
	public List<Field> resources;
}
