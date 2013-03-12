package com.googlecode.fascinator.portal.report.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.googlecode.fascinator.common.BasicHttpClient;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.portal.services.FascinatorService;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;

import org.apache.commons.httpclient.methods.GetMethod;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportStats implements FascinatorService {

	private Logger log = LoggerFactory.getLogger(ReportStats.class);
	private JsonSimple config;
	private HashMap<String, Stat> statMap;
	@Override
	public JsonSimple getConfig() {
		return config;
	}

	@Override
	public void setConfig(JsonSimple config) {
		this.config = config;		
	}

	@Override
	public void init() {
		log.debug("Initializing ReportStats...");		
		statMap = new HashMap<String, Stat>();
		JSONArray stats = config.getArray("config", "stats");
		int idx = 0;
		if (stats!=null) {			
			for (Object json: stats) {
				JsonObject jsonStat = (JsonObject) json;
				log.debug(jsonStat.toJSONString());
				String name = (String)jsonStat.get("name");
				Stat stat = null;
				if (name.startsWith("redbox")) {
					String query = (String)jsonStat.get("query");							
					List<String> fq = config.getStringList("config", "stats", idx, "params", "fq");
					String rows = config.getString("0", "config", "stats", idx, "params", "rows");
					stat = new Stat(name, query, fq, rows);
					JSONArray fieldsArray = config.getArray("config", "stats", idx, "fields");				
					for (Object fieldObj : fieldsArray) {					
						JsonObject field = (JsonObject) fieldObj;
						String fldName = (String) field.get("name");
						String label = (String) field.get("label");
						String solr_field = (String) field.get("solr_field");
						String solr_field_value = (String) field.get("solr_field_value");					
						StatResult result = new StatResult(solr_field+":"+solr_field_value, fldName, label, solr_field, solr_field_value);
						stat.addResult(result);
					}
				} else {
					String url = new JsonSimple(jsonStat).getString("http://localhost:9001/mint", "url");
					stat = new Stat(name, url);
					JSONArray fieldsArray = config.getArray("config", "stats", idx, "fields");				
					for (Object fieldObj : fieldsArray) {					
						JsonObject field = (JsonObject) fieldObj;
						String fldName = (String) field.get("name");
						String label = (String) field.get("label");
						String solr_field = (String) field.get("solr_field");
						String solr_field_value = (String) field.get("solr_field_value");					
						StatResult result = new StatResult(solr_field, fldName, label, solr_field, solr_field_value);
						stat.addResult(result);
					}
				}												
				statMap.put(name, stat);
				
				++idx;
			}
		}
		log.debug("Initialized ReportStats.");
	}
	/** 
	 * Return map of stats based on system configuration.
	 * 
	 * @return Map of Stats
	 */
	public HashMap<String, Stat> getStatCounts(Indexer indexer, String customQuery) throws Exception {	
		for (String key : statMap.keySet()) {
			Stat stat = statMap.get(key);
			stat.resetCounts();
			String query = (customQuery==null ? stat.getQuery() : customQuery);
			if (key.startsWith("mint")) {
				getMintStats(stat, customQuery);
			} else {
				getRedboxStats(indexer, stat, query);
			}
		}
		for (String statKey : statMap.keySet()) {
			log.debug("For stat key: " + statKey);
			Stat stat = statMap.get(statKey);
			for (String resKey : stat.getResults().keySet()) {
				StatResult statRes = stat.getResults().get(resKey);
				log.debug("Result label:" + statRes.getLabel() + " has counts: " + statRes.getCounts());
			}
		}
		return statMap;
	}
	
	private void getMintStats(Stat stat, String query) throws IOException  {
		String targetUrl = stat.getUrl(); //+ "appendFilter="+URLEncoder.encode(query, "utf-8");
		log.debug("Using url"+targetUrl);
		BasicHttpClient client = new BasicHttpClient(targetUrl);
        GetMethod get = new GetMethod(targetUrl);
        client.executeMethod(get);

        JsonSimple mintResult = new JsonSimple(get.getResponseBodyAsString());
        for (String fldKey : stat.getFields()) {
        	StatResult result = stat.getResults().get(fldKey);
        	if (fldKey.indexOf(":") >= 0) {
        		JSONArray groupList = mintResult.getArray(fldKey);
        		for (Object entry : groupList) {
        			String groupName = (String) entry;
        			int counts = mintResult.getInteger(new Integer("0"), fldKey+"counts", groupName).intValue();
        			result.getGroupMap().put(groupName, new Integer(counts));
        			result.setCounts(result.getCounts()+counts);
        		}
        	} else {
        		int counts = mintResult.getInteger(new Integer(-1), fldKey);
        		result.setCounts(counts);
        	}
        }
	}

	private void getRedboxStats(Indexer indexer, Stat stat, String query)
			throws IndexerException, IOException {
		log.debug("Using query:" + query);
		SearchRequest request = new SearchRequest(query);
		int start = 0;
		int pageSize = Integer.valueOf(stat.getRows());
		request.setParam("fq", stat.getFq());
		request.setParam("rows", ""+pageSize);
		request.setParam("start", ""+start);
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		indexer.search(request, result);
		SolrResult resultObject = new SolrResult(new ByteArrayInputStream(result.toByteArray()));
		int numFound = resultObject.getNumFound();
		stat.setResultCount("numFound:", numFound);
		log.debug("numFound:"+numFound);			
		while (true) {
		    List<SolrDoc> results = resultObject.getResults();
		    for (SolrDoc docObject : results) {
		        for (String fldKey : stat.getFields()) {
		        	if (!fldKey.equals("numFound")) {	                		
		        		String value = docObject.getString(null, fldKey);
		        		String resultKey = null;
		        		if (value == null) {
		        			JSONArray valueArr = docObject.getArray(fldKey);
		        			if (valueArr != null) {
		        				value = (String) valueArr.get(0);	                				
		        			} else {	                			  
		        				log.error("value not found for key:" + fldKey);
		        			}
		        		}
		        		resultKey = fldKey+":"+value;
		        		StatResult statRes = stat.getResults().get(resultKey);
		        		if (statRes != null) {
		        			statRes.incCounts();
		        		} else {	                				                			
		        			log.debug("Key not found in map:" + resultKey);
		        		}
		        	}
		        }
		    }
		    start += pageSize;
		    if (start > numFound) {
		        break;
		    }
		    request.setParam("start", "" + start);
		    result = new ByteArrayOutputStream();
		    indexer.search(request, result);
		    resultObject = new SolrResult(new ByteArrayInputStream(result.toByteArray()));
		}
	}
			
}