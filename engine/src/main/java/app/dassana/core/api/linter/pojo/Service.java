package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Service {
	@JsonProperty
	private String id;
	@JsonProperty("resource-types")
	private List<Field> resources;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Field> getResources() {
		return resources;
	}

	public void setResources(List<Field> resources) {
		this.resources = resources;
	}
}
