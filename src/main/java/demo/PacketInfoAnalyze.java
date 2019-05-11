package demo;

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

public class PacketInfoAnalyze {
    private Map<Integer, Integer> packetId2Count = new TreeMap<>();
    private final static String PATTERN = "[recv|send]+\\s(\\d+)";

    public static void main(String[] args) {
        new PacketInfoAnalyze().doAnalyze(99996);
    }

    private void doAnalyze(int port) {
        String url = createUrlByPortAndDate(port);
        try(InputStream in = getInputStreamByUrl(url);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        ) {
            String str = null;
            while ((str = br.readLine()) != null) {
                calcCount(str);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        printResult();
        writeRecord();
    }

    /**
     * 根据端口号和当前日期拼接URL
     * @param port
     * @return
     */
    private  String createUrlByPortAndDate(int port) {
        Date date = new Date();
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sd.format(date);
        StringBuilder sb = new StringBuilder();
        sb.append("http://s").append(port).append(".37wan.cqs.6wtx.com:8888/system/37wan/S")
                .append(port).append("/").append(dateStr).append("/packet.log.").append(dateStr);
        return sb.toString();
    }

    /**
     * 获取网络文件的输入流
     * @param urlStr
     * @return
     */
    private InputStream getInputStreamByUrl(String urlStr){
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

    /**
     * 统计消息包频率
     * @return
     */
    private void calcCount(String info) {
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(info);
        if(matcher.find()){
            Integer packetId = Integer.valueOf(matcher.group(1));
            int preCount = packetId2Count.getOrDefault(packetId, 0);
            packetId2Count.put(packetId, ++preCount);
        }else {
            System.out.println("NO MATCH!");
        }
    }

    /**
     * 打印结果
     * @return
     */
    private void printResult() {
        StringBuilder sb = new StringBuilder();
        packetId2Count.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue()
                        .reversed()).forEach(entry -> {
            sb.append("packetId ").append(entry.getKey()).append(" : ").append("count ").append(entry.getValue());
            System.out.println(sb.toString());
            sb.setLength(0);
        });
        System.out.println("Total number of message package types is"+ packetId2Count.keySet().size());
    }

    private String getInfo(StringBuilder sb, Map.Entry entry) {
        sb.append("packetId ").append(entry.getKey()).append(" : ").append("count ").append(entry.getValue());
        return sb.toString();
    }

    /**
     * 消息包频率统计文件写入
     * @return
     */
    private void writeRecord() {
        String rootPath = System.getProperty("user.dir");
        String fileName =  "record.txt";
        Path path = Paths.get(rootPath + File.separator + fileName);
        try(BufferedWriter writer = Files.newBufferedWriter(path)) {
            StringBuilder sb = new StringBuilder();
            packetId2Count.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue()
                            .reversed()).forEach(entry -> {
                try {
                    writer.write(getInfo(sb, entry));
                    writer.write(System.getProperty("line.separator"));
                    sb.setLength(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }catch (Exception e) {
            e.printStackTrace();
        }

    }




}
