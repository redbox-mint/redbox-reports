package com.googlecode.fascinator.portal.report.service;

import java.util.HashMap;

public class StatResult {		
		private String key;
		private String name;
		private String label;
		private String solrField;
		private String solrFieldValue;
		private int counts;
		private HashMap<String, Integer> groupMap;
		
		public StatResult(String key, String name, String label, String field, String value) {
			this.key = key;
			this.name = name;
			this.label = label;
			this.solrField = field;
			this.solrFieldValue = value;
			this.counts = 0;
			groupMap = new HashMap<String, Integer>();
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getSolrField() {
			return solrField;
		}

		public void setSolrField(String solrField) {
			this.solrField = solrField;
		}

		public String getSolrFieldValue() {
			return solrFieldValue;
		}

		public void setSolrFieldValue(String solrFieldValue) {
			this.solrFieldValue = solrFieldValue;
		}
		
		public int getCounts() {
			return counts;
		}
		
		public void setCounts(int counts) {
			this.counts = counts;
		}
		
		public void incCounts() {
			this.counts++;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public HashMap<String, Integer> getGroupMap() {
			return groupMap;
		}

		public void setGroupMap(HashMap<String, Integer> groupMap) {
			this.groupMap = groupMap;
		}
	}