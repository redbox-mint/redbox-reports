package com.googlecode.fascinator.portal.report.type;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class RecordsPublishedByMonthChartHandler implements ChartHandler {

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
    
    public RecordsPublishedByMonthChartHandler() {
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
    	Map<Integer, Integer> monthCountMap = getData();
        String dataType = "2012 - Records \n Published by \n Month";
        chartData
                .addEntry(monthCountMap.get(Calendar.JANUARY), dataType, "Jan");
        chartData.addEntry(monthCountMap.get(Calendar.FEBRUARY), dataType,
                "Feb");
        chartData.addEntry(monthCountMap.get(Calendar.MARCH), dataType, "Mar");
        chartData.addEntry(monthCountMap.get(Calendar.APRIL), dataType, "Apr");
        chartData.addEntry(monthCountMap.get(Calendar.MAY), dataType, "May");
        chartData.addEntry(monthCountMap.get(Calendar.JUNE), dataType, "Jun");
        chartData.addEntry(monthCountMap.get(Calendar.JULY), dataType, "Jul");
        chartData.addEntry(monthCountMap.get(Calendar.AUGUST), dataType, "Aug");
        chartData.addEntry(monthCountMap.get(Calendar.SEPTEMBER), dataType,
                "Sep");
        chartData
                .addEntry(monthCountMap.get(Calendar.OCTOBER), dataType, "Oct");
        chartData.addEntry(monthCountMap.get(Calendar.NOVEMBER), dataType,
                "Nov");
        chartData.addEntry(monthCountMap.get(Calendar.DECEMBER), dataType,
                "Dec");

        ChartGenerator
                .renderPNGBarChart(outputStream, (BarChartData) chartData);
    }

	private Map<Integer, Integer> getData() throws IndexerException,
			IOException {
		DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat displayDateFormat = new SimpleDateFormat("d/M/yyyy");
        
        dateFromStr = displayDateFormat.format(fromDate);
        dateToStr = displayDateFormat.format(toDate);
        
        ((BarChartData) chartData).setTitle(dateFromStr
                + " to " + dateToStr
                + "\n Records Published By Month");

        Indexer indexer = scriptingServices.getIndexer();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        query += " AND workflow_step:live AND date_created:["
                + solrDateFormat.format(fromDate) + "T00:00:00.000Z TO "
                + solrDateFormat.format(toDate) + "T23:59:59.999Z]";
        SearchRequest request = new SearchRequest(query);
        int start = 0;
        int pageSize = 10;
        request.setParam("start", "" + start);
        request.setParam("rows", "" + pageSize);
        indexer.search(request, result);
        SolrResult resultObject = new SolrResult(result.toString());
        int numFound = resultObject.getNumFound();
        List<String> publishedOids = new ArrayList<String>();

        while (true) {
            List<SolrDoc> results = resultObject.getResults();
            for (SolrDoc docObject : results) {
                publishedOids.add(docObject.get("storage_id"));
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

        Map<Integer, Integer> monthCountMap = new HashMap<Integer, Integer>();

        if (publishedOids.size() > 0) {
            String eventLogQuery = "oid:(";
            for (int i = 0; i < publishedOids.size(); i++) {
                String publishedOid = publishedOids.get(i);
                eventLogQuery += publishedOid;
                if (i < publishedOids.size() - 1) {
                    eventLogQuery += " OR ";
                }
            }
            eventLogQuery += ") AND eventType:\"Publication flag set\"";

            request = new SearchRequest(eventLogQuery);
            result = new ByteArrayOutputStream();
            start = 0;
            pageSize = 10;
            request.setParam("start", "" + start);
            request.setParam("rows", "" + pageSize);
            indexer.searchByIndex(request, result, "eventLog");
            resultObject = new SolrResult(result.toString());
            numFound = resultObject.getNumFound();

            while (true) {
                List<SolrDoc> results = resultObject.getResults();
                for (SolrDoc docObject : results) {
                    String eventTimeString = docObject.getString(null,
                            "eventTime");
                    try {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(solrDateFormat.parse(eventTimeString));
                        int month = calendar.get(Calendar.MONTH);
                        if (monthCountMap.get(month) == null) {
                            monthCountMap.put(month, 1);
                        } else {
                            monthCountMap.put(month,
                                    monthCountMap.get(month) + 1);
                        }

                    } catch (ParseException e) {
                        throw new RuntimeException(e);
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
        }
		return monthCountMap;
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
    	Map<Integer, Integer> countMap = getData();
    	writer.write("Chart Name,Records Published by Month");
    	writer.write(System.getProperty("line.separator"));
    	writer.write("Date From,");
    	writer.write(dateFromStr);
    	writer.write(System.getProperty("line.separator"));
    	writer.write("Date To,");
    	writer.write(dateToStr);
    	writer.write(System.getProperty("line.separator"));
    	JsonObject labelConfig = systemConfig.getObject("charts", chartKey, "csv-field-label");
    	Integer[] flds = new Integer[]{Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL, Calendar.MAY, Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER};
    	for (Integer fldKey : flds) {
			writer.write(getLabel(String.valueOf(fldKey), labelConfig));
    		writer.write(",");
    		writer.write((countMap.get(fldKey) == null ? "0" : countMap.get(fldKey).toString()));
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
