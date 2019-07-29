package demo3.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    private final static String PACKET_LOG_S_S = ">packet.log.([\\s\\S]*?)<";

    public static List<String> getPacket(int port, String datePattern) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        try(InputStream in = StreamUtils.getInputStreamByUrl(createUrlByPortAndDate(port, datePattern));
            BufferedInputStream fis = new BufferedInputStream(in);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "utf-8"), 5 * 10 * 1024 * 1024)// 用50M的缓冲读取文本文件
        ) {{
            String str = "";
            while ((str = reader.readLine()) != null) {
                sb.append(str);

            }
        }} catch (IOException e) {
            e.printStackTrace();
        }
        String content = sb.toString();
        //System.out.println(content);
        Pattern pattern = Pattern.compile(PACKET_LOG_S_S);
        Matcher matcher = pattern.matcher(content);
        while(matcher.find()) {
            String group = matcher.group();
            group = StringUtils.substringBetween(group, "log.", "<");
            //System.out.println(group);
            list.add(group);
        }
        return list;
    }

    public static String createUrlByPortAndDate(int port, String datePattern) {
        String dateStr = getDate(datePattern);
        StringBuilder sb = new StringBuilder();
        sb.append("http://s").append(port).append(".37wan.cqs.6wtx.com:8888/log/37wan/S")
                .append(port).append("/").append(dateStr);
        return sb.toString();
    }

    public static String getDate(String datePattern) {
        Date date = new Date();
        SimpleDateFormat sd;
        if (StringUtils.isBlank(datePattern)) {
            sd = new SimpleDateFormat("yyyy-MM-dd");
        }else {
            sd = new SimpleDateFormat(datePattern);
        }
        return sd.format(date);
    }
}
