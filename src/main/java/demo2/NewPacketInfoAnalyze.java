package demo2;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewPacketInfoAnalyze {

    private Map<Integer, Integer> packetId2Count = new TreeMap<>();

    private final static String PATTERN = "[recv|send]+\\s(\\d+)";
    private final static String PACKET_LOG_S_S = ">packet.log.([\\s\\S]*?)<";

    private void doAnalyze(int port, String datePattern) {
        NewPacketInfoAnalyze newPacketInfoAnalyze = new NewPacketInfoAnalyze();
        List<String> packet = newPacketInfoAnalyze.getPacket(port, datePattern);
        System.out.println("the size is " + packet.size());
        for (String str : packet) {
            long t1 = System.currentTimeMillis();
            String url = newPacketInfoAnalyze.createUrlByPortAndDate(99101, datePattern) + "/packet.log." + str;
            newPacketInfoAnalyze.doAnalyze0(url);
            System.out.println(str);
            System.out.println("cost" + (System.currentTimeMillis() - t1) / 1000L + "s");
        }
        newPacketInfoAnalyze.write(datePattern);
    }

    private String createUrlByPortAndDate(int port, String datePattern) {
        Date date = new Date();
        SimpleDateFormat sd;
        if (StringUtils.isBlank(datePattern)) {
            sd = new SimpleDateFormat("yyyy-MM-dd");
        }else {
            sd = new SimpleDateFormat(datePattern);
        }
        String dateStr = sd.format(date);
        StringBuilder sb = new StringBuilder();
        sb.append("http://s").append(port).append(".37wan.cqs.6wtx.com:8888/log/37wan/S")
                .append(port).append("/").append(dateStr);
        return sb.toString();
    }

    private InputStream getInputStreamByUrl(String urlStr) {
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

    private List<String> getPacket(int port, String datePattern) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        try(InputStream in = getInputStreamByUrl(createUrlByPortAndDate(port, datePattern));
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

    public static void main(String[] args) {
        String datePattern = null;
        int port = 99101;
        new NewPacketInfoAnalyze().doAnalyze(port, datePattern);

    }

    private void doAnalyze0(String url) {
        try (InputStream in = getInputStreamByUrl(url);
             BufferedInputStream fis = new BufferedInputStream(in);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "utf-8"), 5 * 10 * 1024 * 1024);// 用50M的缓冲读取文本文件
        ) {
            String str = "";
            while ((str = reader.readLine()) != null) {
                calcCount(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //printResult();
    }

    private void calcCount(String info) {
        Pattern pattern = Pattern.compile(PATTERN );
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            Integer packetId = Integer.valueOf(matcher.group(1));
            int preCount = packetId2Count.getOrDefault(packetId, 0);
            packetId2Count.put(packetId, ++preCount);
        } else {
            System.out.println("NO MATCH!");
        }
    }

    private void printResult() {
        StringBuilder sb = new StringBuilder();
        packetId2Count.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue()
                        .reversed()).forEach(entry -> {
            sb.append("packetId ").append(entry.getKey()).append(" : ").append("count ").append(entry.getValue());
            System.out.println(sb.toString());
            sb.setLength(0);
        });
        System.out.println("Total number of message package types is " + packetId2Count.keySet().size());
    }

    private void writeRecord(String datePattern) {
        String rootPath = System.getProperty("user.dir");
        String fileName = getDate(datePattern) + "_record.txt";
        Path path = Paths.get(rootPath + File.separator + fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            StringBuilder sb = new StringBuilder();
            packetId2Count.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue()
                            .reversed()).forEach(entry -> {
                try {
                    writer.write(getInfo(sb, entry.getKey(), entry.getValue()));
                    writer.write(System.getProperty("line.separator"));
                    sb.setLength(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getDate(String datePattern) {
        Date date = new Date();
        SimpleDateFormat sd;
        if (StringUtils.isBlank(datePattern)) {
            sd = new SimpleDateFormat("yyyy-MM-dd");
        }else {
            sd = new SimpleDateFormat(datePattern);
        }
        return sd.format(date);
    }

    private String getInfo(StringBuilder sb, int key, int value) {
        sb.append("packetId ").append(key).append(" : ").append("count ").append(value);
        return sb.toString();
    }

    private void write(String datePattern) {
        this.writeRecord(datePattern);
        printResult();
    }
}
