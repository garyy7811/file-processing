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
public class CSReportingSlideViewStats extends CSReportingStatsCommon{

    @Autowired
    private DynamoLoginInfoDAO loginInfoDAO;

    private static final Logger logger = LogManager.getLogger( CSReportingSlideViewStats.class );

    public CSReportingSlideViewStats( SolrClient solrClient ){
        this.solrClient = solrClient;
    }

    private SolrClient solrClient;

    private static String[] columns = new String[]{

            "val",//slide id
            "count",//slide count
            //            "slideName",
            //            "ownerFirst",
            //            "ownerLast",
            //            "ownerEmail",
            "total_dura", "uniq_viewerNum", "total_presNum", "uniq_slideNum", "uniq_presNum", "uniq_showNum" };


    public Object[] queryTotal( String csSessionId, Object[] groupIds, Long timeFrom, Long timeTo )
            throws IOException, SolrServerException, IllegalAccessException{

        loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        if( groupIds.length == 0 ){
            throw new IllegalArgumentException( "noGroupIdFound" );
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery( "*:*" );
        solrQuery.setRows( 0 );
        String beanType = "CSReportingSlideDurationSolr";
        solrQuery.setFilterQueries(
                "javaClassType: " + beanType + " AND slideId__l_t_t_f:[ 0 TO * ] " + " AND ownerGroupId__l_t_t_f:(" +
                        StringUtils.arrayToDelimitedString( groupIds, " OR " ) + ")" );

        long gap = timeTo - timeFrom;

        String tmp = "{\n" + getRangeNumsJsonStr( "nn", timeFrom, timeTo ) + ",\n" +
                getRangeNumsJsonStr( "pp", timeFrom - gap, timeFrom ) + ",\n lc:{" + "\n        type:terms," +
                "\n        field:slideIsLib__b_t_t_f" + "\n }" + "\n}";
        solrQuery.add( "json.facet", tmp );

        QueryResponse queryResponse = solrClient.query( solrQuery );
        SimpleOrderedMap facetResult = ( ( SimpleOrderedMap )queryResponse.getResponse().get( "facets" ) );
        if( facetResult == null || facetResult.get( "count" ).equals( 0 ) ){
            return new Object[]{ new HashMap<>(), new HashMap<>() };
        }
        HashMap<String, Serializable> nn = fillResultRecord( ( SimpleOrderedMap )facetResult.get( "nn" ) );
        HashMap<String, Serializable> pp = fillResultRecord( ( SimpleOrderedMap )facetResult.get( "pp" ) );
        List<SimpleOrderedMap> o = ( List )( ( SimpleOrderedMap )facetResult.get( "lc" ) ).get( "buckets" );
        HashMap<Boolean, Integer> libShare = new HashMap<>();
        for( SimpleOrderedMap m : o ){
            libShare.put( ( Boolean )m.get( "val" ), ( Integer )m.get( "count" ) );
        }
        return new Object[]{ nn, pp, libShare };
    }

    private String getRangeNumsJsonStr( String qname, long tf, long tt ){
        return "    " + qname + ":{" + "\n        type: query," + "\n        q:\"startTime__l_t_t_f:[" + tf + " TO " +
                tt + "]\" ," + "\n        facet: {" +

                "\n             uniq_slideNum: \"unique(slideId__l_t_t_f)\"," +
                "\n             uniq_presNum: \"unique(presentationId__l_t_t_f)\"," +
                "\n             uniq_showNum: \"unique(slideShowId__l_t_t_f)\"" +


                "\n        }" + "\n    }";
    }

    private static String[] queryContainingFields =
            new String[]{ "query_ownerFirstName__sw_t_f_f", "query_ownerLastName__sw_t_f_f",
                    "query_ownerEmail__sw_t_f_f", "query_slideRefName__sw_t_f_f" };

    public DtoFacetResult<HashMap<String, Serializable>, Serializable> queryGrid( String csSessionId, Object[] groupIds,
                                                                                  Long timeFrom, Long timeTo,
                                                                                  String sortCol, Boolean solrDesc,
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
        String beanType = "CSReportingSlideDurationSolr";
        String filterStr =
                "javaClassType: " + beanType + " AND startTime__l_t_t_f:[ " + timeFrom + " TO " + timeTo + " ] " +
                        " AND slideId__l_t_t_f:[ 0 TO * ] " + " AND ownerGroupId__l_t_t_f:(" +
                        StringUtils.arrayToDelimitedString( groupIds, " OR " ) + ")";

        solrQuery.setFilterQueries( filterStr );

        solrQuery.add( "json.facet",
                "\n{" + "\n     slideViews: {" + "\n         type: terms," + "\n         field: slideId__l_t_t_f," +
                        "\n         sort: {" + sortCol + ": " + ( solrDesc ? "desc" : "asc" ) + "}," +
                        "\n         offset: " + offset + "," + "\n         limit: " + limit + "," +
                        "\n         facet: {" + "\n             total_dura: \"sum(duration__l_t_t_f)\"," +
                        "\n             uniq_viewerNum: \"unique(userId__l_t_t_f)\"," +
                        "\n             total_presNum: \"unique(slideRefId__l_t_t_f)\"," +
                        "\n             uniq_presNum: \"unique(presentationId__l_t_t_f)\"," +
                        "\n             uniq_showNum: \"unique(slideShowId__l_t_t_f)\"" +/*
                        "\n             slideName:{" +
                        "\n                 type:terms," +
                        "\n                 field:slideRefName__si_t_t_f," +
                        "\n                 limit:1," +
                        "\n                 latest:\"max(startTime__l_t_t_f)\"," +
                        "\n                 sort:{latest:desc}" +
                        "\n             }" +*/
                        "\n         }" + "\n     }," + "\n     total: {" + "\n         type: query," +
                        "\n         q: \"*:*\"," + "\n         facet: {" +
                        "\n             uniq_slide_id: \"unique(slideId__l_t_t_f)\"" + "\n         }" + "\n     }" +
                        "\n}" );

        QueryResponse queryResponse = solrClient.query( solrQuery );
        SimpleOrderedMap facetResult = ( ( SimpleOrderedMap )queryResponse.getResponse().get( "facets" ) );
        if( facetResult == null || facetResult.get( "count" ).equals( 0 ) ){
            return new DtoFacetResult<>();
        }

        DtoFacetResult<HashMap<String, Serializable>, Serializable> rt = new DtoFacetResult<>();
        SimpleOrderedMap showidtotalmap = ( SimpleOrderedMap )facetResult.get( "total" );
        rt.setrNumFound( ( Integer )showidtotalmap.get( "uniq_slide_id" ) );

        SimpleOrderedMap facetResultStats = ( SimpleOrderedMap )facetResult.get( "slideViews" );
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
            tmp.put( column, value );
        }
        return tmp;
    }

}
