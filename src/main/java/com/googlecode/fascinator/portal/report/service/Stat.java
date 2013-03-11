package com.googlecode.fascinator.portal.report.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.fascinator.portal.report.service.StatResult;

public class Stat {
		private String name;
		private String query;
		private List<String> fq;
		private HashMap<String, StatResult> results;
		private Set<String> fields;
		private HashMap<String, StatResult> resultsByName;
		private String rows;
		private String url;
		
		public Stat(String name, String query, List<String> fq, String rows) {
			results = new HashMap<String, StatResult>();
			resultsByName = new HashMap<String, StatResult>();
			fields = new HashSet<String>();
			this.name = name;
			this.query = query;
			this.fq = fq;
			this.rows = rows;
		}
		
		public Stat(String name, String url) {
			this.name = name;
			this.url = url;
			results = new HashMap<String, StatResult>();
			resultsByName = new HashMap<String, StatResult>();
			fields = new HashSet<String>();
		}
		
		public void resetCounts() {
			for (String key:results.keySet()) {
				StatResult statResult = results.get(key);
				statResult.setCounts(0);
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public List<String> getFq() {
			return fq;
		}

		public void setFq(List<String> fq) {
			this.fq = fq;
		}

		public Map<String, StatResult> getResults() {
			return results;
		}

		public String getRows() {
			return rows;
		}

		public void setRows(String rows) {
			this.rows = rows;
		}
		
		public void addResult(StatResult result) {
			results.put(result.getKey(), result);
			fields.add(result.getSolrField());
			resultsByName.put(result.getName(), result);
		}
		
		public void setResultCount(String key, int counts) {
			results.get(key).setCounts(counts);
		}
		
		public Set<String> getFields() {
			return fields;
		}
		
		public StatResult getResultByName(String name) {
			return resultsByName.get(name);
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
}