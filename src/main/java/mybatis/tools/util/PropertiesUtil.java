package mybatis.tools.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Created by jzq1999 on 2017/4/12.
 */
public class PropertiesUtil {

    private static Properties prop = new Properties();

    public static String FILE_PATH = "filePath";

    static {
        try {
            InputStream in = PropertiesUtil.class.getResourceAsStream("/database.properties");
            prop.load(in);
            Set keyValue = prop.keySet();
            for (Iterator it = keyValue.iterator(); it.hasNext();)
            {
                String key = (String) it.next();
                System.out.println("key:" + key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getValue(String key){
        String value = prop.getProperty(key);

        return value;
    }

}
