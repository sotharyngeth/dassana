package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Policies {
	@JsonProperty
	public List<Policy> classes;
}
