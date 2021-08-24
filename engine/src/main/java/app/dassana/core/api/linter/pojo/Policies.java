package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Policies {
	@JsonProperty
	private List<Policy> classes;

	public List<Policy> getClasses() {
		return classes;
	}

	public void setClasses(List<Policy> classes) {
		this.classes = classes;
	}
}
