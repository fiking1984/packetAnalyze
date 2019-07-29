package demo3.utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class StreamUtils {

    /**
     * 异常捕获和资源释放交由上层调用者处理
     * @param urlStr
     * @return
     */
    public static InputStream getInputStreamByUrl(String urlStr) {
        //System.out.println(urlStr);
        DataInputStream in = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            in = new DataInputStream(conn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }
}
