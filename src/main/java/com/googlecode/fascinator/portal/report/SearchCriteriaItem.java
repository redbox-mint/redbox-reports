package com.googlecode.fascinator.portal.report;

public class SearchCriteriaItem {
	private String operator;
	private String value;
	private String field;
	private String allowNulls;
	private String matchingOperator;
	private String solr_field;

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getAllowNulls() {
		return allowNulls;
	}

	public void setAllowNulls(String allowNulls) {
		this.allowNulls = allowNulls;
	}

	public String getMatchingOperator() {
		return matchingOperator;
	}

	public void setMatchingOperator(String matchingOperator) {
		this.matchingOperator = matchingOperator;
	}

	public String getSolr_field() {
		return solr_field;
	}

	public void setSolr_field(String solr_field) {
		this.solr_field = solr_field;
	}

}