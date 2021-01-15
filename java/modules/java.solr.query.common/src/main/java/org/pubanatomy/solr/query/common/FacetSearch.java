package org.pubanatomy.solr.query.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gary.yang.customshow on 5/28/2015.
 */
public class FacetSearch<hitType extends Serializable> implements Serializable{

    private String queryStr;

    private String filterStr;

    private String[] columns;

    private List<hitType> hits;

    private HashMap<String, FacetSearch<HashMap<String, Number>>> stats;

}
