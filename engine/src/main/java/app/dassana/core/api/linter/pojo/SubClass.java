package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SubClass {
	@JsonProperty
	public String id;
	@JsonProperty
	public List<Category> categories;
}
