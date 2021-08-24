package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Policy {
	@JsonProperty
	private String id;
	@JsonProperty
	private List<SubClass> subclasses;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<SubClass> getSubclasses() {
		return subclasses;
	}

	public void setSubclasses(List<SubClass> subclasses) {
		this.subclasses = subclasses;
	}
}
