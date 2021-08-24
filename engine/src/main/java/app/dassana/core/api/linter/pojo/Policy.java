package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Policy {
	@JsonProperty
	public String id;
	@JsonProperty
	public List<SubClass> subclasses;
	//public Object subclasses;
	//


}
