package org.pubanatomy.videotranscoding;

import org.springframework.http.HttpEntity;

/**
 * User: flashflexpro@gmail.com
 * Date: 3/5/2016
 * Time: 7:02 PM
 */
public interface EncodingComAPI{

    String sendInJson( HttpEntity httpRequest );

}
