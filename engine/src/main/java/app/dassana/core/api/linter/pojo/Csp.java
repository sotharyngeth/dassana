package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Csp {
	@JsonProperty("csp")
	public List<Provider> providers;
}
