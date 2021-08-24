package app.dassana.core.api.linter.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Category {
	@JsonProperty
	private String id;
	@JsonProperty
	private List<Field> subcategories;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Field> getSubcategories() {
		return subcategories;
	}

	public void setSubcategories(List<Field> subcategories) {
		this.subcategories = subcategories;
	}
}
