package org.pubanatomy.solr.query.web.common;

import java.io.Serializable;
import java.util.HashMap;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 2:32 PM
 */
public class DtoFacetResult<DtoT extends Serializable, StatsT extends Object> implements Serializable{

    private String queryStr;
    private long   qStartRow;
    private long   qRowNum;
    private int qMaxFacetTermNum;
    private String[] qFacetFields;


    private long rStartRow;

    private long rNumFound;
    private DtoT[] rQueryRecords;

    private HashMap<String, HashMap<String, StatsT>> rFacetsRecords;


    public String getQueryStr(){
        return queryStr;
    }

    public void setQueryStr( String queryStr ){
        this.queryStr = queryStr;
    }

    public long getqStartRow(){
        return qStartRow;
    }

    public void setqStartRow( long qStartRow ){
        this.qStartRow = qStartRow;
    }

    public long getqRowNum(){
        return qRowNum;
    }

    public void setqRowNum( long qRowNum ){
        this.qRowNum = qRowNum;
    }

    public String[] getqFacetFields(){
        return qFacetFields;
    }

    public void setqFacetFields( String[] qFacetFields ){
        this.qFacetFields = qFacetFields;
    }

    public int getqMaxFacetTermNum(){
        return qMaxFacetTermNum;
    }

    public void setqMaxFacetTermNum( int qMaxFacetTermNum ){
        this.qMaxFacetTermNum = qMaxFacetTermNum;
    }

    public long getrNumFound(){
        return rNumFound;
    }

    public void setrNumFound( long rNumFound ){
        this.rNumFound = rNumFound;
    }

    public long getrStartRow(){
        return rStartRow;
    }

    public void setrStartRow( long rStartRow ){
        this.rStartRow = rStartRow;
    }

    public DtoT[] getrQueryRecords(){
        return rQueryRecords;
    }

    public void setrQueryRecords( DtoT[] rQueryRecords ){
        this.rQueryRecords = rQueryRecords;
    }

    public HashMap<String, HashMap<String, StatsT>> getrFacetsRecords(){
        return rFacetsRecords;
    }

    public void setrFacetsRecords( HashMap<String, HashMap<String, StatsT>> rFacetsRecords ){
        this.rFacetsRecords = rFacetsRecords;
    }
}
