package org.pubanatomy.reporting.solr.query;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by gary.yang.customshow on 5/27/2015.
 */
@Service
public class CSReportingStatsCommon{
    private static final Logger logger = LogManager.getLogger( CSReportingStatsCommon.class );

    protected String mixFieldsExactAndContains( String[] containingFields, String[] terms ){
        logger.debug( "terms:{}, containing:{}, exact:{}", terms, containingFields );

        List<String> fctLst = new LinkedList<>();
        for( int i = 0; i < terms.length; i++ ){
            String term = terms[ i ];
            if( term.startsWith( "\"" ) && term.endsWith( "\"" ) ){
                logger.debug( "quotes:{}", term );
                term = term.substring( 1, term.length() - 1 );
                term = escapeTerm( term, true );
                for( int j = 0; j < containingFields.length; j++ ){
                    fctLst.add( containingFields[ j ] + ":*" + term + "*" );
                }
            }
            else{
                if( term.length() > 0 ){
                    term = escapeTerm( term, false );

                    logger.debug( "ctarr:{}", term );
                    String[] ctarr = term.split( " " );
                    for( int cj = 0; cj < ctarr.length; cj++ ){
                        String s = ctarr[ cj ];
                        if( s.length() > 0 && ! " ".equals( s ) ){
                            for( int j = 0; j < containingFields.length; j++ ){
                                fctLst.add( containingFields[ j ] + ":*" + s + "*" );
                            }
                        }
                    }
                }
            }
        }

        final String rt = StringUtils.collectionToDelimitedString( fctLst, " OR " );
        logger.debug( "return:{}", rt );
        return rt;
    }

    private String escapeTerm( String term, boolean s ){
        term = term.toLowerCase();
        StringBuilder sb = new StringBuilder();
        for( char c : term.toCharArray() ){
            switch( c ){
                case '+':
                case '-':
                case '!':
                case '(':
                case ')':
                case '{':
                case '}':
                case '[':
                case ']':
                case '^':
                case '"':
                case '~':
                case ':':
                case '/':
                case '*':
                case '?':
                    sb.append( '\\' );
                case ' ':
                    if( s ){
                        sb.append( '\\' );
                    }
                default:
                    sb.append( c );
            }
        }
        term = sb.toString();
        term = term.replaceAll( "\\&\\&", "\\\\&\\\\&" );
        term = term.replaceAll( "\\|\\|", "\\\\|\\\\|" );
        return term;
    }


}
