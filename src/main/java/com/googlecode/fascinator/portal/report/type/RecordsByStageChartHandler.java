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

public class RecordsByStageChartHandler implements ChartHandler {

    private ScriptingServices scriptingServices;
    private JsonSimple systemConfig;
    private ChartData chartData;
    private String query = "*:*";
    private int imgW = 550;
    private int imgH = 400;
    private Date fromDate = null;
    private Date toDate = null;
    private String titleStr = "";
    private String headerStr = "Records by Workflow Stage";
    private String dateFromStr = "";
    private String dateToStr = "";
    
    public RecordsByStageChartHandler() {
        chartData = new BarChartData("", "", "", BarChartData.LabelPos.SLANTED,
                BarChartData.LabelPos.HIDDEN, imgW, imgH, false);
        ((BarChartData) chartData).setBaseSeriesColor(new Color(98, 157, 209));
    }

    @Override
    public Date getFromDate() {
        return fromDate;
    }

    @Override
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @Override
    public Date getToDate() {
        return toDate;
    }

    @Override
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @Override
    public void renderChart(OutputStream outputStream) throws IOException,
            IndexerException {
        Map<String, Integer> stepCountMap = getData();
        
        ((BarChartData) chartData).setTitle(titleStr);
        
        chartData.addEntry(stepCountMap.get("inbox"), "", "Inbox");
        chartData.addEntry(stepCountMap.get("investigation"), "",
                "Investigation");
        chartData.addEntry(stepCountMap.get("metadata-review"), "", "Metadata Review");
        chartData
                .addEntry(stepCountMap.get("final-review"), "", "Final Review");
        chartData.addEntry(stepCountMap.get("live"), "", "Published");
        chartData.addEntry(stepCountMap.get("retired"), "", "Retired");

        ChartGenerator
                .renderPNGBarChart(outputStream, (BarChartData) chartData);
    }

	private Map<String, Integer> getData() throws IndexerException, IOException {
		DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat displayDateFormat = new SimpleDateFormat("d/M/yyyy");
        dateFromStr = displayDateFormat.format(fromDate);
        dateToStr = displayDateFormat.format(toDate);
        titleStr = dateFromStr
                + " to " + dateToStr
                + "\n "+headerStr;
        

        Map<String, Integer> stepCountMap = new HashMap<String, Integer>();
        stepCountMap.put("inbox", 0);
        stepCountMap.put("investigation", 0);
        stepCountMap.put("metadata-review", 0);
        stepCountMap.put("final-review", 0);
        stepCountMap.put("live", 0);
        stepCountMap.put("retired", 0);

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
                        Integer count = stepCountMap.get(workflowStep);
                        if (count != null) {
                            stepCountMap.put(workflowStep, ++count);
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

    @Override
    public void setImgW(int imgW) {
        ((BarChartData) chartData).setImgW(imgW);
    }

    @Override
    public void setImgH(int imgH) {
        ((BarChartData) chartData).setImgH(imgH);
    }

    @Override
    public void setScriptingServices(ScriptingServices scriptingServices) {
        this.scriptingServices = scriptingServices;
    }

    @Override
    public void setSystemConfig(JsonSimple systemConfig) {
        this.systemConfig = systemConfig;
    }
    
    @Override
    public void renderCsv(Writer writer, String chartKey) throws IOException, IndexerException {
    	Map<String, Integer> stepCountMap = getData();
    	writer.write("Chart Name,");
    	writer.write(headerStr);
    	writer.write(System.getProperty("line.separator"));
    	writer.write("Date From,");
    	writer.write(dateFromStr);
    	writer.write(System.getProperty("line.separator"));
    	writer.write("Date To,");
    	writer.write(dateToStr);
    	writer.write(System.getProperty("line.separator"));
    	JsonObject labelConfig = systemConfig.getObject("charts", chartKey, "csv-field-label");
    	String[] flds = new String[]{"inbox", "investigation", "metadata-review", "final-review", "live", "retired"};
    	for (String fldKey : flds) {
    		writer.write(getLabel(fldKey, labelConfig));
    		writer.write(",");
    		writer.write(stepCountMap.get(fldKey).toString());
    		writer.write(System.getProperty("line.separator"));
    	}
    }
    
    private String getLabel(String key, JsonObject labelConfig) {
    	if (labelConfig.get(key) == null) {
			return key;
		}
		return (String)labelConfig.get(key);
    }
}
