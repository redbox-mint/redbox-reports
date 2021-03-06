package com.googlecode.fascinator.portal.report.type;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.httpclient.methods.GetMethod;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.BasicHttpClient;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.solr.SolrResult;
import com.googlecode.fascinator.portal.report.BarChartData;
import com.googlecode.fascinator.portal.report.ChartData;
import com.googlecode.fascinator.portal.report.ChartGenerator;
import com.googlecode.fascinator.portal.services.ScriptingServices;
import com.googlecode.fascinator.portal.report.type.ChartHandler;

public class PublishedRecordsByTypeChartHandler implements ChartHandler {

    private ScriptingServices scriptingServices;
    
    private ChartData chartData;
    private String query = "*:*";
    private int imgW = 550;
    private int imgH = 400;
    private Date fromDate = null;
    private Date toDate = null;
    private JsonSimple systemConfig;
    private String dateFromStr = "";
    private String dateToStr = "";

    public PublishedRecordsByTypeChartHandler() {
        chartData = new BarChartData("", "", "", BarChartData.LabelPos.HIDDEN,
                BarChartData.LabelPos.LEFT, imgW, imgH, false);
        ((BarChartData) chartData).setUseSeriesColor(true);
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
    	DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int datasetCount = getRbData(solrDateFormat);

        JsonSimple mintResult = getMintData(solrDateFormat);

        chartData.addEntry(
                mintResult.getInteger(0, "Parties_Groups")
                        + mintResult.getInteger(0, "Parties_People"), "Party",
                "Published Records", new Color(98, 157, 209));
        chartData.addEntry(datasetCount, "Collection", "Published Records",
                new Color(41, 127, 213));
        chartData.addEntry(mintResult.getInteger(0, "Activities"), "Activity",
                "Published Records", new Color(127, 143, 169));
        chartData.addEntry(mintResult.getInteger(0, "Services"), "Service",
                "Published Records", new Color(45, 127, 217));

        ChartGenerator
                .renderPNGBarChart(outputStream, (BarChartData) chartData);
    }

	private JsonSimple getMintData(DateFormat solrDateFormat)
			throws IOException {
		String url = systemConfig.getString("http://localhost:9001/mint",
                "proxy-urls", "Published_Records_By_Type")
                + "&dateFrom="
                + solrDateFormat.format(fromDate)
                + "&dateTo="
                + solrDateFormat.format(toDate);
        BasicHttpClient client = new BasicHttpClient(url);
        GetMethod get = new GetMethod(url);
        client.executeMethod(get);

        JsonSimple mintResult = new JsonSimple(get.getResponseBodyAsString());
		return mintResult;
	}

	private int getRbData(DateFormat solrDateFormat) throws IndexerException,
			IOException {
		DateFormat displayDateFormat = new SimpleDateFormat("d/M/yyyy");
        
		dateFromStr = displayDateFormat.format(fromDate);
        dateToStr = displayDateFormat.format(toDate);
        
        ((BarChartData) chartData).setTitle(displayDateFormat.format(fromDate)
                + " to " + displayDateFormat.format(toDate)
                + "\n Published Records by Record Type");

        Indexer indexer = scriptingServices.getIndexer();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        query += " AND published:true AND oai_set:default AND date_created:["
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
        int datasetCount = 0;

        while (true) {
            datasetCount += resultObject.getResults().size();

            start += pageSize;
            if (start > numFound) {
                break;
            }
            request.setParam("start", "" + start);
            result = new ByteArrayOutputStream();
            indexer.search(request, result);
            resultObject = new SolrResult(result.toString());
        }
		return datasetCount;
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
    	DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int datasetCount = getRbData(solrDateFormat);
        JsonSimple mintResult = getMintData(solrDateFormat); 
    	writer.write("Chart Name,Published Records by Record Type");
    	writer.write(System.getProperty("line.separator"));
    	writer.write("Date From,");
    	writer.write(dateFromStr);
    	writer.write(System.getProperty("line.separator"));
    	writer.write("Date To,");
    	writer.write(dateToStr);
    	writer.write(System.getProperty("line.separator"));
    	JsonObject labelConfig = systemConfig.getObject("charts", chartKey, "csv-field-label");
    	writer.write(getLabel("party", labelConfig));
    	writer.write(",");
    	writer.write(mintResult.getInteger(0, "Parties").toString());
    	writer.write(System.getProperty("line.separator"));
    	writer.write(getLabel("collection", labelConfig));
    	writer.write(",");
    	writer.write(String.valueOf(datasetCount));
    	writer.write(System.getProperty("line.separator"));
    	writer.write(getLabel("activity", labelConfig));
    	writer.write(",");
    	writer.write(mintResult.getInteger(0, "Activities").toString());
    	writer.write(System.getProperty("line.separator"));
    	writer.write(getLabel("service", labelConfig));
    	writer.write(",");
    	writer.write(mintResult.getInteger(0, "Services").toString());
    	writer.write(System.getProperty("line.separator"));
    	
    	 chartData.addEntry(
                 mintResult.getInteger(0, "Parties")
                         + mintResult.getInteger(0, "Parties_People"), "Party",
                 "Published Records", new Color(98, 157, 209));
         chartData.addEntry(datasetCount, "Collection", "Published Records",
                 new Color(41, 127, 213));
         chartData.addEntry(mintResult.getInteger(0, "Activities"), "Activity",
                 "Published Records", new Color(127, 143, 169));
         chartData.addEntry(mintResult.getInteger(0, "Services"), "Service",
                 "Published Records", new Color(45, 127, 217));
    }
    
    private String getLabel(String key, JsonObject labelConfig) {
    	if (labelConfig.get(key) == null) {
			return key;
		}
		return (String)labelConfig.get(key);
    }
}
