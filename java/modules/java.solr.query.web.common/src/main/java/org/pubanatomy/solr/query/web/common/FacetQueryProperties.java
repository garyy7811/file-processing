package org.pubanatomy.solr.query.web.common;

import org.pubanatomy.loginverify.DynaLogInSessionInfo;
import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import org.pubanatomy.solr.query.common.CopySolrFields;
import org.pubanatomy.solr.query.common.FacetQuerySolr;
import org.pubanatomy.solr.query.common.SolrBean;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 5/2/13
 * Time: 2:50 PM
 */
public class FacetQueryProperties<SolrBeanT extends SolrBean, DtoT extends Serializable>{

    @Autowired
    private DynamoLoginInfoDAO loginInfoDAO;

    protected FacetQueryProperties( FacetQuerySolr<SolrBeanT> facetSearching ){
        this.facetSearching = facetSearching;
    }

    protected FacetQuerySolr<SolrBeanT> facetSearching;

    public DtoFacetResult<DtoT, Long> facetQuery( String csSessionId, String queryStr, Integer startRow, Integer rowNum,
                                                  HashMap field2offsetLimitPrefixSort, String sortBy, Boolean isDesc )
            throws ParserConfigurationException, SAXException, IOException, SolrServerException, IllegalAccessException {
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );

        FacetQuerySolr.FacetSearchResult<SolrBeanT, Long> facetResult = facetSearching
                .searchWithFacets( queryStr, startRow, rowNum, field2offsetLimitPrefixSort, sortBy, isDesc );
        return facetResultToDto( facetResult );
    }

    protected DtoFacetResult<DtoT, Long> facetResultToDto(
            FacetQuerySolr.FacetSearchResult<SolrBeanT, Long> searchResult ){
        DtoFacetResult<DtoT, Long> dsf = new DtoFacetResult();

        dsf.setQueryStr( searchResult.getQueryStr() );
        dsf.setqStartRow( searchResult.getqStartRow() );
        dsf.setqRowNum( searchResult.getqRowNum() );
        dsf.setqFacetFields( searchResult.getqFacetFields() );
        dsf.setqMaxFacetTermNum( searchResult.getqMaxFacetTermNum() );

        dsf.setrNumFound( searchResult.getrNumFound() );
        dsf.setrStartRow( searchResult.getrStartRow() );
        ArrayList<Serializable> lst = new ArrayList<>();
        for( SolrBean r : searchResult.getrQueryRecords() ){
            lst.add( beanToDto( r ) );
        }
        dsf.setrQueryRecords( ( DtoT[] )lst.toArray( new Serializable[ lst.size() ] ) );

        if( searchResult.getrFacetsRecords() != null ){
            dsf.setrFacetsRecords( statsToDTO( searchResult ) );
        }
        return dsf;

    }

    protected HashMap<String, HashMap<String, Long>> statsToDTO(
            FacetQuerySolr.FacetSearchResult<SolrBeanT, Long> searchResult ){
        HashMap<String, HashMap<String, Long>> frLst = new HashMap<>();
        for( Map.Entry<String, HashMap<String, Long>> f2t2c : searchResult.getrFacetsRecords().entrySet() ){
            HashMap<String, Long> t2m = new HashMap<>();
            for( Map.Entry<String, Long> tnEntry : f2t2c.getValue().entrySet() ){
                t2m.put( tnEntry.getKey(), tnEntry.getValue() );
            }
            frLst.put( f2t2c.getKey(), t2m );
        }
        return frLst;
    }


    protected DtoFacetResultRecord beanToDto( SolrBean s ){
        HashMap<String, Object> map = new HashMap<>();
        CopySolrFields.convertToMap( s, map );
        DtoFacetResultRecord dtoFacetResultRecord = new DtoFacetResultRecord();
        dtoFacetResultRecord.setUid( ( String )map.get( "uid" ) );
        dtoFacetResultRecord.setProperties( map );
        return dtoFacetResultRecord;
    }


    public HashMap suggest( String csSessionId, String fieldName, String prefixStr, int termsLimit )
            throws SAXException, ParserConfigurationException, SolrServerException, IOException, IllegalAccessException {
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        return facetSearching.getTopTermsByPrefix( fieldName, prefixStr, termsLimit );
    }

    public long count( String csSessionId, String queryStr )
            throws SAXException, ParserConfigurationException, SolrServerException, IOException, IllegalAccessException {
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        return facetSearching.getQuickCount( queryStr );
    }


}
