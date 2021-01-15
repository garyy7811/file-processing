package org.pubanatomy.reporting.solr.query;


import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import org.pubanatomy.solr.query.web.common.DtoFacetResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gary.yang.customshow on 5/27/2015.
 */
@Service
public class CSReportingShowSessionStats extends CSReportingStatsCommon{


    @Autowired
    private DynamoLoginInfoDAO loginInfoDAO;

    private static final Logger logger = LogManager.getLogger( CSReportingShowSessionStats.class );

    public CSReportingShowSessionStats( SolrClient solrClient ){
        this.solrClient = solrClient;
    }

    private SolrClient solrClient;

    private static String[] columns = new String[]{

            "val", "count",
            //            "showName",
            //            "ownerFirst",
            //            "ownerLast",
            //            "ownerEmail",
            "avg_dura", "avg_viewedSlideNum", "avg_slideNum", "lastUpdateTime", "lastViewedTime", "createdTime",
            "uniq_viewerNum"

    };


    public Object[] queryTotal( String csSessionId, Object[] groupIds, Long timeFrom, Long timeTo )
            throws IOException, SolrServerException, IllegalAccessException{
        loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        if( groupIds.length == 0 ){
            throw new IllegalArgumentException( "noGroupIdFound" );
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery( "*:*" );
        solrQuery.setRows( 0 );
        String beanType = "CSReportingShowDurationSolr";
        solrQuery.setFilterQueries( "javaClassType: " + beanType + " AND ownerGroupId__l_t_t_f:(" +
                StringUtils.arrayToDelimitedString( groupIds, " OR " ) + ")" );

        long gap = timeTo - timeFrom;

        String tmp = "{\n" + getRangeNumsJsonStr( "nn", timeFrom, timeTo ) + ",\n" +
                getRangeNumsJsonStr( "pp", timeFrom - gap, timeFrom ) + "}";
        solrQuery.add( "json.facet", tmp );

        QueryResponse queryResponse = solrClient.query( solrQuery );
        SimpleOrderedMap facetResult = ( ( SimpleOrderedMap )queryResponse.getResponse().get( "facets" ) );
        if( facetResult == null || facetResult.get( "count" ).equals( 0 ) ){
            return new Object[]{ new HashMap<>(), new HashMap<>() };
        }
        HashMap<String, Serializable> nn = fillResultRecord( ( SimpleOrderedMap )facetResult.get( "nn" ) );
        HashMap<String, Serializable> pp = fillResultRecord( ( SimpleOrderedMap )facetResult.get( "pp" ) );
        return new Object[]{ nn, pp };

    }

    private String getRangeNumsJsonStr( String qname, long tf, long tt ){
        return "    " + qname + ":{" + "\n        type: query," + "\n        q:\"startTime__l_t_t_f:[" + tf + " TO " +
                tt + "]\" ," + "\n        facet: {" + "\n            avg_dura: \"avg(duration__l_t_t_f)\"," +
                "\n            avg_viewedSlideNum: \"avg(viewedSlideNum__i_t_t_f)\"," +
                "\n            avg_slideNum: \"avg(slideNum__i_t_t_f)\"," +
                "\n            lastUpdateTime: \"max(lastUpdatedTime__l_t_t_f)\"," +
                "\n            lastViewedTime: \"max(startTime__l_t_t_f)\"," +
                "\n            uniq_viewerNum: \"unique(userId__l_t_t_f)\"," +
                "\n            createdTime: \"max(createTime__l_t_t_f)\"" + "\n        }" + "\n    }";
    }

    private static String[] queryContainingFields =
            new String[]{ "query_ownerFirstName__sw_t_f_f", "query_ownerLastName__sw_t_f_f",
                    "query_ownerEmail__sw_t_f_f", "query_slideShowName__sw_t_f_f" };

    public DtoFacetResult<HashMap<String, Serializable>, Serializable> queryGrid( String csSessionId, Object[] groupIds,
                                                                                  Long timeFrom, Long timeTo,
                                                                                  String sortCol, Boolean desc,
                                                                                  int offset, int limit,
                                                                                  String[] filterContains )
            throws IOException, SolrServerException, IllegalAccessException{

        loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        if( groupIds.length == 0 ){
            throw new IllegalArgumentException( "noGroupIdFound" );
        }

        SolrQuery solrQuery = new SolrQuery();
        if( filterContains != null && filterContains.length > 0 ){
            solrQuery.setQuery(
                    mixFieldsExactAndContains( queryContainingFields, filterContains ) );
        }
        else{
            solrQuery.setQuery( "*:*" );
        }
        solrQuery.setRows( 0 );
        String beanType = "CSReportingShowDurationSolr";
        String filterStr =
                "javaClassType: " + beanType + " AND startTime__l_t_t_f:[ " + timeFrom + " TO " + timeTo + " ] " +
                        " AND ownerGroupId__l_t_t_f:(" + StringUtils.arrayToDelimitedString( groupIds, " OR " ) + ")";

        solrQuery.setFilterQueries( filterStr );

        solrQuery.add( "json.facet",
                "\n{" + "\n    slideshow: {" + "\n        type: terms," + "\n        field: slideShowId__l_t_t_f," +
                        "\n        sort: {" + sortCol + ": " + ( desc ? "desc" : "asc" ) + "}," + "\n        offset: " +
                        offset + "," + "\n        limit: " + limit + "," + "\n        facet: {" +
                        "\n            avg_dura: \"avg(duration__l_t_t_f)\"," +
                        "\n            avg_viewedSlideNum: \"avg(viewedSlideNum__i_t_t_f)\"," +
                        "\n            avg_slideNum: \"avg(slideNum__i_t_t_f)\"," +
                        "\n            lastUpdateTime: \"max(lastUpdatedTime__l_t_t_f)\"," +
                        "\n            lastViewedTime: \"max(startTime__l_t_t_f)\"," +
                        "\n            uniq_viewerNum: \"unique(userId__l_t_t_f)\"," +
                        "\n            createdTime: \"max(createTime__l_t_t_f)\"" +/*
                        "\n            showName:{" +
                        "\n                 type:terms," +
                        "\n                 field:slideShowName__si_t_t_f," +
                        "\n                 limit:1," +
                        "\n                 lastUpdatedTime:\"max(lastUpdatedTime__l_t_t_f)\"," +
                        "\n                 sort:{lastUpdatedTime:desc}" +
                        "\n            }," +
                        "\n            ownerFirst:{" +
                        "\n                 type:terms," +
                        "\n                 field:ownerFirstName__si_t_t_f," +
                        "\n                 limit:1," +
                        "\n                 lastUpdatedTime:\"max(lastUpdatedTime__l_t_t_f)\"," +
                        "\n                 sort:{lastUpdatedTime:desc}" +
                        "\n            }," +
                        "\n            ownerLast:{" +
                        "\n                 type:terms," +
                        "\n                 field:ownerLastName__si_t_t_f," +
                        "\n                 limit:1," +
                        "\n                 lastUpdatedTime:\"max(lastUpdatedTime__l_t_t_f)\"," +
                        "\n                 sort:{lastUpdatedTime:desc}" +
                        "\n            }," +
                        "\n            ownerEmail:{" +
                        "\n                 type:terms," +
                        "\n                 field:ownerEmail__si_t_t_f," +
                        "\n                 limit:1," +
                        "\n                 lastUpdatedTime:\"max(lastUpdatedTime__l_t_t_f)\"," +
                        "\n                 sort:{lastUpdatedTime:desc}" +
                        "\n            }" +*/
                        "\n        }" + "\n    }," + "\n    total: {" + "\n        type: query," +
                        "\n        q: \"*:*\"," + "\n        facet: {" +
                        "\n            uniq_show_id: \"unique(slideShowId__l_t_t_f)\"" + "\n        }" + "\n    }" +
                        "\n}" );

        QueryResponse queryResponse = solrClient.query( solrQuery );
        SimpleOrderedMap facetResult = ( ( SimpleOrderedMap )queryResponse.getResponse().get( "facets" ) );
        if( facetResult == null || facetResult.get( "count" ).equals( 0 ) ){
            return new DtoFacetResult<>();
        }

        DtoFacetResult<HashMap<String, Serializable>, Serializable> rt = new DtoFacetResult<>();
        SimpleOrderedMap showidtotalmap = ( SimpleOrderedMap )facetResult.get( "total" );
        rt.setrNumFound( ( Integer )showidtotalmap.get( "uniq_show_id" ) );

        SimpleOrderedMap facetResultStats = ( SimpleOrderedMap )facetResult.get( "slideshow" );
        if( facetResultStats != null ){
            List<SimpleOrderedMap> facetResultStatsLst = ( List<SimpleOrderedMap> )facetResultStats.get( "buckets" );
            HashMap<String, Serializable>[] rQueryRecords = new HashMap[ facetResultStatsLst.size() ];
            for( int i = 0; i < facetResultStatsLst.size(); i++ ){
                rQueryRecords[ i ] = fillResultRecord( facetResultStatsLst.get( i ) );
            }
            rt.setrQueryRecords( rQueryRecords );
        }

        logger.debug( ".query end" );
        return rt;
    }

    private HashMap<String, Serializable> fillResultRecord( SimpleOrderedMap each ){
        HashMap<String, Serializable> tmp = new HashMap<>();
        for( String column : columns ){
            Serializable value = ( Serializable )each.get( column );
            if( value instanceof Double && ! column.equals( "avg_viewedSlideNum" ) ){
                value = new Double( Math.ceil( ( Double )value ) ).longValue();
            }
            else if( value instanceof SimpleOrderedMap ){
                SimpleOrderedMap subVal = ( SimpleOrderedMap )value;
                value = ( Serializable )( ( SimpleOrderedMap )( ( List )subVal.get( "buckets" ) ).get( 0 ) )
                        .get( "val" );
            }
            tmp.put( column, value );
        }
        return tmp;
    }

}
