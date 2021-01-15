import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class LabyrinthCopyResAndThumbToS3{

    private static ClassPathXmlApplicationContext applicationContext;

    public static void main( String[] args ){
        applicationContext = new ClassPathXmlApplicationContext( "root-context.xml" );
        System.out.println( ">>>>>>>" + applicationContext.getApplicationName() );
    }

}
