package org.pubanatomy.siutils;

import org.apache.logging.log4j.LogManager;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * User: GaryY
 * Date: 12/27/2016
 */
public class FlashLogging{

    private static SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss,SSS Z" );

    public boolean log( String sessionId, Object[] logArr ){
        for( int i = 0; i < logArr.length; i++ ){
            List args = ( List )logArr[ i ];
            log( sessionId, ( String )args.get( 0 ), ( String )args.get( 1 ), ( String )args.get( 2 ),
                    ( String )args.get( 3 ), ( Date )args.get( 4 ), ( Boolean )args.get( 5 ), ( Integer )args.get( 6 ),
                    ( String )args.get( 7 ), ( String )args.get( 8 ) );
        }
        return true;
    }

    private static final String[] LOG_LEVELS = new String[]{ "debug", "info", "warn", "error", "fatal" };

    private boolean log( String sessionId, String logName, String envConfig, String csVersion, String buildStr,
                         Date clientDate, boolean mainThread, int logLevel, String logMsg, String stack ){
        if( StringUtils.isEmpty( logName ) || StringUtils.isEmpty( envConfig ) || StringUtils.isEmpty( csVersion ) ||
                StringUtils.isEmpty( buildStr ) || StringUtils.isEmpty( logMsg ) || StringUtils.isEmpty( stack ) ){
            throw new RuntimeException( "some string is null !" );
        }


        ParseStack parseStack = new ParseStack( stack.split( "\n\t" )[ 2 ] );
        try{
            parseStack.invoke();
        }
        catch( Exception e ){
            logMsg += stack;
        }

        String asClass = parseStack.getAsClass();
        String asMethod = parseStack.getAsMethod();
        String asFilePath = parseStack.getAsFilePath();
        String asLineNum = parseStack.getAsFileLineNum();
        String logLevelStr = LOG_LEVELS[ logLevel ];
        if( StringUtils.isEmpty( asClass ) || StringUtils.isEmpty( asMethod ) || StringUtils.isEmpty( logLevelStr ) ){
            throw new RuntimeException( "error parsing stack:\n" + stack );
        }

        final String message =
                "∈" + sdf.format( clientDate ) + "∋∈" + logName + "∋∈" + envConfig + "∋∈" + csVersion + "∋∈" +
                        buildStr + "∋∈" + mainThread + "∋∈" + asClass + "∋∈" + asMethod + "∋∈" + asFilePath + "∋∈" +
                        asLineNum + "∋∈" + logLevelStr + "∋∈" + logMsg + "∋";
        LogManager.getLogger().fatal( message );
        System.out.println( ">>\n" + message + "\n<<" );
        return true;
    }

    private class ParseStack{
        private String stack         = "";
        private String asClass       = "";
        private String asMethod      = "";
        private String asFileLineNum = "100";
        private String asFilePath    = "unavailable.as";

        public ParseStack( String s ){
            this.stack = s;
        }

        public String getAsClass(){
            return asClass;
        }

        public String getAsMethod(){
            return asMethod;
        }

        public String getAsFileLineNum(){
            return asFileLineNum;
        }

        public String getAsFilePath(){
            return asFilePath;
        }

        public void invoke(){
            String cline = stack;
            String[] ccArr = cline.split( "\\(\\)\\[" );

            String[] clsFnc = ccArr[ 0 ].substring( 3 ).split( "/" );
            asClass = clsFnc[ 0 ].replaceAll( "::", "." );
            asMethod = clsFnc[ 1 ];

            if( ccArr.length > 1 ){
                String[] flLnArr = ccArr[ 1 ].split( ":" );
                String tmp = flLnArr[ flLnArr.length - 1 ];
                asFileLineNum = tmp.substring( 0, tmp.length() - 2 );
                asFilePath = ccArr[ 1 ].substring( 0, ccArr[ 1 ].length() - tmp.length() - 1 );
                String[] farr = asFilePath.contains( "/" ) ? asFilePath.split( "/" ) : asFilePath.split( "\\\\" );
                asFilePath = farr[ farr.length - 1 ];
            }
            else if( asMethod.endsWith( "()" ) ){
                asMethod = asMethod.substring( 0, asMethod.length() - 2 );
            }

        }
    }

}
