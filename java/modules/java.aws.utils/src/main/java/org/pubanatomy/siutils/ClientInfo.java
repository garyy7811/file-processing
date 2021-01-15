package org.pubanatomy.siutils;

import java.lang.annotation.*;

/**
 * User: flashflexpro@gmail.com
 * Date: 4/2/2016
 * Time: 12:06 PM
 */
@Target( { ElementType.FIELD, ElementType.TYPE } )
@Retention( RetentionPolicy.RUNTIME )
@Inherited
public @interface ClientInfo{

    boolean notEmpty() default false;

    boolean readOnly() default false;

    String stringRegexp() default "";

    String[] enumStrings() default {};
}
