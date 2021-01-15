package com.customshow.labyrinth.fontgen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.HashMap;

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM
 */
public class FontPlugin implements Plugin<Project>{


    @Override
    public void apply( Project target ){
        System.out.println( ">>>>> ...");
        HashMap<String, Object> args = new HashMap<>();

        args.put( "type", FindMissingFonts.class );
        target.task( args, FindMissingFonts.NAME );


        args.put( "type", LoadAllCsFontsRes.class );
        target.task( args, LoadAllCsFontsRes.NAME );
        System.out.println( "<<<<<< ...");

    }
}
