package com.googlecode.fascinator.portal.report.type;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;
import com.googlecode.fascinator.portal.report.BarChartData;
import com.googlecode.fascinator.portal.report.ChartData;
import com.googlecode.fascinator.portal.report.ChartGenerator;
import com.googlecode.fascinator.portal.services.ScriptingServices;
import com.googlecode.fascinator.portal.report.type.ChartHandler;

public class RecordsByStage2ChartHandler implements ChartHandler {

    private ScriptingServices scriptingServices;
    private JsonSimple systemConfig;
    private ChartData chartData;
    private String query = "*:*";
    private int imgW = 550;
    private int imgH = 400;
    private Date fromDate = null;
    private Date toDate = null;
    private String dateFromStr = "";
    private String dateToStr = "";

    public RecordsByStage2ChartHandler() {
        BarChartData chartData = new BarChartData("", "", "",
        		BarChartData.LabelPos.SLANTED, BarChartData.LabelPos.RIGHT,
                imgW, imgH, true);
        chartData.setUseSeriesColor(true);
        this.chartData = chartData;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public void renderChart(OutputStream outputStream) throws IOException,
            IndexerException {

    	Map<String, Map<String, Integer>> stepCountMap = getData();

        Color clrIdx = new Color(18, 45, 69);
        Color clrRep = new Color(18, 101, 69);
        Color clrReg = new Color(89, 45, 85);
        Color clrCol = new Color(89, 100, 85);
        Color clrDat = new Color(23, 106, 113);
        // due to the series concept in JFreeChart, we'll add all unique rows
        // first to set the colors
        // TODO: refactor to remove this limitation
        // investigation counts
        chartData.addEntry(
                stepCountMap.get("investigation").get("catalogueOrIndex"),
                "Catalogue/Index", "Investigation", clrIdx);
        chartData.addEntry(stepCountMap.get("investigation").get("repository"),
                "Repository", "Investigation", clrRep);
        chartData.addEntry(stepCountMap.get("investigation").get("dataset"),
                "Dataset", "Investigation", clrDat);
        chartData.addEntry(stepCountMap.get("investigation").get("collection"),
                "Collection", "Investigation", clrCol);
        chartData.addEntry(stepCountMap.get("investigation").get("registry"),
                "Registry", "Investigation", clrReg);

        // metadata-review counts
        chartData.addEntry(
                stepCountMap.get("metadata-review").get("catalogueOrIndex"),
                "Catalogue/Index", "Metadata Review", clrIdx);
        chartData.addEntry(stepCountMap.get("metadata-review")
                .get("repository"), "Repository", "Metadata Review",
                clrRep);
        chartData.addEntry(stepCountMap.get("metadata-review").get("dataset"),
                "Dataset", "Metadata Review", clrDat);
        chartData.addEntry(stepCountMap.get("metadata-review")
                .get("collection"), "Collection", "Metadata Review", clrCol);
        chartData.addEntry(stepCountMap.get("metadata-review").get("registry"),
                "Registry", "Metadata Review", clrReg);

        // final-review counts
        chartData.addEntry(
                stepCountMap.get("final-review").get("catalogueOrIndex"),
                "Catalogue/Index", "Final Review", clrIdx);
        chartData.addEntry(stepCountMap.get("final-review").get("repository"),
                "Repository", "Final Review", clrRep);
        chartData.addEntry(stepCountMap.get("final-review").get("dataset"),
                "Dataset", "Final Review", clrDat);
        chartData.addEntry(stepCountMap.get("final-review").get("collection"),
                "Collection", "Final Review", clrCol);
        chartData.addEntry(stepCountMap.get("final-review").get("registry"),
                "Registry", "Final Review", clrReg);

        // live counts
        chartData.addEntry(stepCountMap.get("live").get("catalogueOrIndex"),
                "Catalogue/Index", "Published", clrIdx);
        chartData.addEntry(stepCountMap.get("live").get("repository"),
                "Repository", "Published", clrRep);
        chartData.addEntry(stepCountMap.get("live").get("dataset"), "Dataset",
                "Published", clrDat);
        chartData.addEntry(stepCountMap.get("live").get("collection"),
                "Collection", "Published", clrCol);
        chartData.addEntry(stepCountMap.get("live").get("registry"),
                "Registry", "Published", clrReg);

        // retired counts
        chartData.addEntry(stepCountMap.get("retired").get("catalogueOrIndex"),
                "Catalogue/Index", "Retired", clrIdx);
        chartData.addEntry(stepCountMap.get("retired").get("repository"),
                "Repository", "Retired", clrRep);
        chartData.addEntry(stepCountMap.get("retired").get("dataset"),
                "Dataset", "Retired", clrDat);
        chartData.addEntry(stepCountMap.get("retired").get("collection"),
                "Collection", "Retired", clrCol);
        chartData.addEntry(stepCountMap.get("retired").get("registry"),
                "Registry", "Retired", clrReg);

        ChartGenerator
                .renderPNGBarChart(outputStream, (BarChartData) chartData);
    }

	private Map<String, Map<String, Integer>> getData()
			throws IndexerException, IOException {
		DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat displayDateFormat = new SimpleDateFormat("d/M/yyyy");
        
        dateFromStr = displayDateFormat.format(fromDate);
        dateToStr = displayDateFormat.format(toDate);
        
        ((BarChartData) chartData).setTitle(dateFromStr
                + " to " + dateToStr
                + "\n Record Type by Workflow Stage");

        Map<String, Map<String, Integer>> stepCountMap = new HashMap<String, Map<String, Integer>>();
        stepCountMap.put("inbox", getDataTypeCountMap());
        stepCountMap.put("investigation", getDataTypeCountMap());
        stepCountMap.put("metadata-review", getDataTypeCountMap());
        stepCountMap.put("final-review", getDataTypeCountMap());
        stepCountMap.put("live", getDataTypeCountMap());
        stepCountMap.put("retired", getDataTypeCountMap());

        Indexer indexer = scriptingServices.getIndexer();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        query += " AND date_created:[" + solrDateFormat.format(fromDate)
                + "T00:00:00.000Z TO " + solrDateFormat.format(toDate)
                + "T23:59:59.999Z]";
        SearchRequest request = new SearchRequest(query);
        int start = 0;
        int pageSize = 10;
        request.setParam("start", "" + start);
        request.setParam("rows", "" + pageSize);
        indexer.search(request, result);
        SolrResult resultObject = new SolrResult(result.toString());
        int numFound = resultObject.getNumFound();
        while (true) {
            List<SolrDoc> results = resultObject.getResults();
            for (SolrDoc docObject : results) {

                JSONArray workflowSteps = docObject.getArray("workflow_step");
                if (workflowSteps != null) {
                    for (int i = 0; i < workflowSteps.size(); i++) {
                        String workflowStep = (String) workflowSteps.get(i);
                        Map<String, Integer> dataTypeCountMap = stepCountMap
                                .get(workflowStep);
                        if (dataTypeCountMap != null) {
                            JSONArray dataTypeArray = docObject
                                    .getArray("dc:type.rdf:PlainLiteral");
                            if (dataTypeArray != null) {
                                for (int j = 0; j < dataTypeArray.size(); j++) {
                                    Integer count = dataTypeCountMap
                                            .get(dataTypeArray.get(j));
                                    if (count != null) {
                                        dataTypeCountMap.put(
                                                (String) dataTypeArray.get(j),
                                                ++count);
                                    }
                                }
                            }
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
            resultObject = new SolrResult(result.toString());
        }
		return stepCountMap;
	}

    private Map<String, Integer> getDataTypeCountMap() {
        Map<String, Integer> dataTypeCountMap = new HashMap<String, Integer>();
        dataTypeCountMap.put("catalogueOrIndex", 0);
        dataTypeCountMap.put("collection", 0);
        dataTypeCountMap.put("dataset", 0);
        dataTypeCountMap.put("registry", 0);
        dataTypeCountMap.put("live", 0);
        dataTypeCountMap.put("repository", 0);
        return dataTypeCountMap;
    }

    public void setImgW(int imgW) {
        ((BarChartData) chartData).setImgW(imgW);
    }

    public void setImgH(int imgH) {
        ((BarChartData) chartData).setImgH(imgH);
    }

    public void setScriptingServices(ScriptingServices scriptingServices) {
        this.scriptingServices = scriptingServices;
    }

    @Override
    public void setSystemConfig(JsonSimple systemConfig) {
        this.systemConfig = systemConfig;
    }

    @Override
    public void renderCsv(Writer writer, String chartKey) throws IOException, IndexerException {
    	Map<String, Map<String, Integer>> countMap = getData();
    	writer.write("Chart Name,Record Type by Workflow Stage");
    	writer.write(System.getProperty("line.separator"));
    	writer.write("Date From,");
    	writer.write(dateFromStr);
    	writer.write(System.getProperty("line.separator"));
    	writer.write("Date To,");
    	writer.write(dateToStr);
    	writer.write(System.getProperty("line.separator"));
    	JsonObject labelConfig = systemConfig.getObject("charts", chartKey, "csv-field-label");
    	String[] flds = new String[]{"inbox", "investigation", "metadata-review", "final-review", "live", "retired"};
    	String[] subFlds = new String[]{"catalogueOrIndex", "repository", "dataset", "collection", "registry"};
    	for (String fldKey : flds) {
    		for (String key : subFlds ) {
    			writer.write(getLabel(fldKey, labelConfig));
        		writer.write(",");
        		writer.write(getLabel(key, labelConfig));
        		writer.write(",");
        		writer.write(countMap.get(fldKey).get(key).toString());
        		writer.write(System.getProperty("line.separator"));
    		}
    	}
    }
    
    private String getLabel(String key, JsonObject labelConfig) {
    	if (labelConfig.get(key) == null) {
			return key;
		}
		return (String)labelConfig.get(key);
    }
}
