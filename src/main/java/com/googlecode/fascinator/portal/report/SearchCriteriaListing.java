package com.googlecode.fascinator.portal.report;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.portal.report.SearchCriteriaItem;

public class SearchCriteriaListing {
	public static final String KEY_DATE_FROM = "dateFrom";
	public static final String KEY_DATE_TO = "dateTO";
	public static final String KEY_SHOW_OPTION = "showOption";
	public static final String KEY_CRITERIA_LOGICAL_OP = "logicalOp";
	public static final String KEY_CRITERIA_SEARCH_COMPONENT = "searchcomponent";
	public static final String KEY_CRITERIA_INCLUDE_NULLS = "include_nulls";
	public static final String KEY_CRITERIA_LOGICAL_OP_AND = "AND";
	public static final String KEY_CRITERIA_LOGICAL_OP_OR = "OR";
	public static final String KEY_CRITERIA_FIELD = "dropdown-input";
	public static final String KEY_CRITERIA_VALUE = "value";
	public static final String KEY_CRITERIA = "report-criteria";

	private String dateFrom;
	private String dateTo;
	private String showOption;
	private ArrayList<SearchCriteriaItem> criteria = new ArrayList<SearchCriteriaItem>();

	public SearchCriteriaListing(RedboxReport rb, JsonSimple config, String strDateFormat, String solrDateFormat) {
		JsonObject queryFilters = config.getObject("query", "filter");
		String[] keyArray = Arrays.copyOf(
				new ArrayList<Object>(queryFilters.keySet()).toArray(),
				queryFilters.keySet().size(), String[].class);
		List<String> keys = Arrays.asList(keyArray);
		java.util.Collections.sort(keys);

		DateFormat queryDateFormatter = new SimpleDateFormat(strDateFormat);
		DateFormat solrDateFormatter = new SimpleDateFormat(solrDateFormat);

		// Firstly, get the basics
		try {
			if (keys.indexOf(KEY_DATE_FROM) != -1) {
				this.dateFrom = solrDateFormatter.format(queryDateFormatter
						.parse((String) ((JsonObject) queryFilters
								.get(KEY_DATE_FROM)).get(KEY_CRITERIA_VALUE)));
			}

			if (keys.indexOf(KEY_DATE_TO) != -1) {
				this.dateFrom = solrDateFormatter.format(queryDateFormatter
						.parse((String) ((JsonObject) queryFilters
								.get(KEY_DATE_TO)).get(KEY_CRITERIA_VALUE)));
			}

			if (keys.indexOf(KEY_SHOW_OPTION) != -1) {
				this.dateFrom = ((String) ((JsonObject) queryFilters
						.get(KEY_SHOW_OPTION)).get(KEY_CRITERIA_VALUE));
			}
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		// Now get each criteria option.
		int i = 1;
		while (true) {
			String item = KEY_CRITERIA + "." + i + ".";
			if (keys.indexOf(item + KEY_CRITERIA_FIELD) == -1) {
				// no more criteria to gather
				break;
			}
			SearchCriteriaItem criteriaItem = new SearchCriteriaItem();

			criteriaItem.setValue(((String) ((JsonObject) queryFilters.get(item
					+ KEY_CRITERIA_SEARCH_COMPONENT)).get(KEY_CRITERIA_VALUE)));

			criteriaItem.setField(((String) ((JsonObject) queryFilters.get(item
					+ KEY_CRITERIA_FIELD)).get(KEY_CRITERIA_VALUE)));

			// Get the SOLR field
			criteriaItem.setSolr_field((String) rb.findJsonObjectWithKey(
					criteriaItem.getField()).get("solrField"));

			if (keys.indexOf(item + "logicalOp") != -1) {
				criteriaItem.setOperator((String) ((JsonObject) queryFilters
					.get(item + "logicalOp")).get("value"));
			} else {
				criteriaItem.setOperator(KEY_CRITERIA_LOGICAL_OP_OR);
			}

			if (keys.indexOf(item + "match_contains") != -1) {
			criteriaItem
					.setMatchingOperator((String) ((JsonObject) queryFilters
							.get(item + "match_contains")).get("value"));
			} else {
				criteriaItem
				.setMatchingOperator("field_match");
			}
			
			if (keys.indexOf(item + "include_nulls") != -1) {
				criteriaItem.setAllowNulls((String) ((JsonObject) queryFilters
					.get(item + "include_nulls")).get("value"));
			} else {
				criteriaItem.setAllowNulls("field_include_null");
			}
			
			this.criteria.add(criteriaItem);
			i++;
		}
	}

	public String getDateFrom() {
		return dateFrom;
	}

	public String getDateTo() {
		return dateTo;
	}

	public String getShowOption() {
		return showOption;
	}

	public ArrayList<SearchCriteriaItem> getCriteria() {
		return this.criteria;
	}
}