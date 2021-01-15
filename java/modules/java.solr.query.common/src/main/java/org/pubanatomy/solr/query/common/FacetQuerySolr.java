package org.pubanatomy.solr.query.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.cache.annotation.Cacheable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author flashflexpro@gmail.com
 *         Date: 4/30/12
 *         Time: 3:55 PM
 */
public class FacetQuerySolr<SolrBeanT extends SolrBean>{


    private static final Logger logger = LogManager.getLogger( FacetQuerySolr.class );

    protected FacetQuerySolr( SolrClient solrClient, Class<SolrBeanT> solrBeanType ) throws IllegalAccessException, InstantiationException{
        this.solrClient = solrClient;
        this.solrBeanType = solrBeanType;
        this.javaClassType = solrBeanType.newInstance().getJavaClassType();
    }

    private SolrClient solrClient;
    private Class<SolrBeanT> solrBeanType;
    private String javaClassType;

    public SolrClient getSolrClient(){
        return solrClient;
    }


    @Cacheable( "searchWithFacets" )
    public FacetSearchResult<SolrBeanT, Long> searchWithFacets( String queryStr, int startRow, int rowNum )
            throws SAXException, ParserConfigurationException, IOException, SolrServerException{
        return searchWithFacets( queryStr, startRow, rowNum, null, null, false );
    }

    public FacetSearchResult<SolrBeanT, Long> searchWithFacets( final String queryStr, int startRow, int rowNum,
                                                                HashMap<String, Object[]> field2offsetLimitPrefixSort, String sortBy,
                                                                boolean desc )
            throws IOException, SAXException, ParserConfigurationException, SolrServerException{

        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery( queryStr );
        solrQuery.setFilterQueries( "javaClassType:" + javaClassType  );
        solrQuery.setStart( startRow );
        solrQuery.setRows( rowNum );

        if( field2offsetLimitPrefixSort != null && field2offsetLimitPrefixSort.size() > 0 ){
            solrQuery.setFacet( true );
            solrQuery.setFacetSort( "count" );
            for( Map.Entry<String, Object[]> field2Config : field2offsetLimitPrefixSort.entrySet() ){
                solrQuery.add( "facet.field", field2Config.getKey() );
                solrQuery.add( "f." + field2Config.getKey() + ".facet.mincount", "1" );
                solrQuery.add( "f." + field2Config.getKey() + ".facet.offset", field2Config.getValue()[ 0 ].toString() );
                solrQuery.add( "f." + field2Config.getKey() + ".facet.limit", field2Config.getValue()[ 1 ].toString() );
                if( field2Config.getValue().length > 2 && field2Config.getValue()[ 2 ] != null ){
                    solrQuery.add( "f." + field2Config.getKey() + ".facet.prefix", field2Config.getValue()[ 2 ].toString() );
                }
                if( field2Config.getValue().length > 3 && field2Config.getValue()[ 3 ] != null ){
                    solrQuery.add( "f." + field2Config.getKey() + ".facet.sort", field2Config.getValue()[ 3 ].toString() );
                }
            }
        }
        if( sortBy != null && sortBy.length() > 0 ){
            solrQuery.addSort( sortBy, desc ? SolrQuery.ORDER.desc : SolrQuery.ORDER.asc );
        }

        if( logger.isDebugEnabled() ){
            logger.debug( "queryStr:" + solrQuery.getQuery() );
        }
        QueryResponse solrResp = solrClient.query( solrQuery );

        if( logger.isDebugEnabled() ){
            logger.debug( " result:" + solrResp.getResults().getNumFound() );
        }


        List<SolrBeanT> beans = solrResp.getBeans( solrBeanType );
        SolrBeanT[] rta = ( SolrBeanT[] ) Array.newInstance( solrBeanType, beans.size() );
        beans.toArray( rta );

        FacetSearchResult<SolrBeanT, Long> rt = new FacetSearchResult<>();

        if( field2offsetLimitPrefixSort != null && field2offsetLimitPrefixSort.size() > 0 ){
            HashMap<String, HashMap<String, Long>> fnt2cRslt = new HashMap<>();
            for( String fn : field2offsetLimitPrefixSort.keySet() ){
                HashMap<String, Long> ft2c = new HashMap<>();
                FacetField lst = solrResp.getFacetField( fn );
                if( lst.getValueCount() > 0 && lst.getValues() != null ){
                    for( FacetField.Count c : lst.getValues() ){
                        ft2c.put( c.getName(), c.getCount() );
                    }
                }
                fnt2cRslt.put( fn, ft2c );
            }
            rt.setrFacetsRecords( fnt2cRslt );
        }


        rt.setQueryStr( queryStr );
        rt.setqStartRow( startRow );
        rt.setqRowNum( rowNum );
        rt.setrNumFound( solrResp.getResults().getNumFound() );
        rt.setrStartRow( solrResp.getResults().getStart() );
        rt.setrQueryRecords( rta );
        return rt;
    }

    @Cacheable( "suggest" )
    public HashMap<String, Long> getTopTermsByPrefix( String fieldName, String prefixStr,
                                                      int termsLimit )
            throws IOException, SAXException, ParserConfigurationException, SolrServerException{
        SolrQuery params = new SolrQuery();
        params.setQuery( "javaClassType:" + this.javaClassType );
        params.setRows( 0 );
        params.setFacet( true );
        params.add( "facet.field", fieldName );


        params.add( "f." + fieldName + ".facet.mincount", "1" );
        params.add( "f." + fieldName + ".facet.offset", "0" );
        params.add( "f." + fieldName + ".facet.limit", termsLimit + "" );
        params.add( "f." + fieldName + ".facet.prefix", prefixStr );
        params.add( "f." + fieldName + ".facet.sort", "count" );

        QueryResponse resp = solrClient.query( params );

        FacetField lst = resp.getFacetField( fieldName );

        HashMap<String, Long> rt = new HashMap<>( lst.getValueCount() );
        if( lst.getValueCount() > 0 && lst.getValues() != null ){
            for( FacetField.Count c : lst.getValues() ){
                rt.put( c.getName(), c.getCount() );
            }
        }

        return rt;
    }


    @Cacheable( "count" )
    public long getQuickCount( String queryStr )
            throws IOException, SAXException, ParserConfigurationException, SolrServerException{
        queryStr = "javaClassType:" + javaClassType + " AND (" + queryStr + ")";
        SolrQuery quickCountQuery = new SolrQuery( queryStr );
        quickCountQuery.setRows( 0 );
        QueryResponse resp = solrClient.query( quickCountQuery );
        return resp.getResults().getNumFound();
    }


    public static class FacetSearchResult<SolrBeanType extends SolrBean, FacetStatsType extends Object> implements Serializable{

        private String queryStr;
        private long qStartRow;
        private long qRowNum;

        private String[] qFacetFields;

        private int qMaxFacetTermNum;


        private long rNumFound;

        private long rStartRow;

        private SolrBeanType[] rQueryRecords;

        private HashMap<String, HashMap<String, FacetStatsType>> rFacetsRecords;


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

        public SolrBeanType[] getrQueryRecords(){
            return rQueryRecords;
        }

        public void setrQueryRecords( SolrBeanType[] rQueryRecords ){
            this.rQueryRecords = rQueryRecords;
        }

        public HashMap<String, HashMap<String, FacetStatsType>> getrFacetsRecords(){
            return rFacetsRecords;
        }

        public void setrFacetsRecords( HashMap<String, HashMap<String, FacetStatsType>> rFacetsRecords ){
            this.rFacetsRecords = rFacetsRecords;
        }
    }
}
