package com.googlecode.fascinator.portal.report;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;

public class StatisticalReport extends Report {

    private static final String AND_OPERATOR = " AND ";
    private String strDateFormat;
    private String solrDateFormat = "yyyy-MM-dd";    

    public StatisticalReport() throws IOException {
        super();
        strDateFormat = "dd/MM/yyyy";        
    }

    public StatisticalReport(String name, String label) throws IOException {
        super(name, label);
        strDateFormat = "dd/MM/yyyy";        
    }

    public StatisticalReport(JsonSimple config) throws IOException {
        super(config);
        strDateFormat = config.getString("dd/MM/yyyy", "report", "dateFormat");
    }

    /**
     * Generates the report specific query from parameters
     */
    @Override
    public String getQueryAsString() {
        String query = "";
        JsonObject queryFilters = config.getObject("query", "filter");
        String[] keyArray = Arrays.copyOf(
                new ArrayList<Object>(queryFilters.keySet()).toArray(),
                queryFilters.keySet().size(), String[].class);
        List<String> keys = Arrays.asList(keyArray);
        java.util.Collections.sort(keys);
        query += processDateCriteria(queryFilters);
        query += processShowCriteria(queryFilters);
        int i = 1;
        while (true) {
            if (keys.indexOf("report-criteria." + i + ".dropdown") == -1) {
                break;
            }            
            i++;
        }
        return query;
    }

    private String processShowCriteria(JsonObject queryFilters) {
        String showOption = (String) ((JsonObject) queryFilters
                .get("showOption")).get("value");
        if ("published".equals(showOption)) {
            return AND_OPERATOR + "published:true";
        }
        return "";
    }

    private String processDateCriteria(JsonObject queryFilters) {
        String dateCriteriaQuery = "";
        String dateType = (String) ((JsonObject) queryFilters
                .get("dateCreatedModified")).get("value");
        if ("created".equals(dateType)) {
            dateCriteriaQuery += "date_created:";
        } else {
            dateCriteriaQuery += "last_modified:";
        }
        DateFormat queryDateFormatter = new SimpleDateFormat(strDateFormat);
        DateFormat solrDateFormatter = new SimpleDateFormat(solrDateFormat);
        String dateFrom, dateTo;
        try {
            dateFrom = solrDateFormatter.format(queryDateFormatter
                    .parse((String) ((JsonObject) queryFilters.get("dateFrom"))
                            .get("value")));

            dateTo = solrDateFormatter.format(queryDateFormatter
                    .parse((String) ((JsonObject) queryFilters.get("dateTo"))
                            .get("value")));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        dateCriteriaQuery += "[" + dateFrom + "T00:00:00.000Z TO " + dateTo
                + "T23:59:59.999Z] ";
        return dateCriteriaQuery;
    }
    

    @Override
    public synchronized String toJsonString() {
        JsonObject reportObj = config.writeObject("report");
        reportObj.put("dateFormat", strDateFormat);
        reportObj.put("className", this.getClass().getName());
        return super.toJsonString();
    }

    public String getStrDateFormat() {
        return strDateFormat;
    }

    public void setStrDateFormat(String strDateFormat) {
        this.strDateFormat = strDateFormat;
    }

    public JsonSimple getConfig() {
        return config;
    }

}