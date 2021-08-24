package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Provider {
	@JsonProperty
	public String id, name;
	@JsonProperty
	public List<Service> services;
}
