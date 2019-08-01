package demo3.model;

import demo3.utils.StreamUtils;

import java.io.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataInfo {

    private final static String PATTERN = "[recv|send]+\\s(\\d+)";

    private Map<Integer, Integer> packetId2Count = new TreeMap<>();
    private final String url;
    private final String datePattern;

    public DataInfo(String url, String datePattern) {
        this.url = url;
        this.datePattern = datePattern;
    }

    public void clear() {
        this.packetId2Count.clear();
    }

    public Map<Integer, Integer> split(int size) {
        if (this.packetId2Count.size() == size) {
            return this.packetId2Count;
        }
        Map<Integer, Integer> map = new HashMap<>();
        packetId2Count.entrySet().stream().limit(size).forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        return map;
    }

    public Void doAnalyze() {
        long t1 = System.currentTimeMillis();
        try (InputStream in = StreamUtils.getInputStreamByUrl(url);
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
        System.out.println(url + "子任务耗时: " + (System.currentTimeMillis() - t1) / 1000L + "s ThreadName: " + Thread.currentThread().getName());
        return null;
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

    public Map<Integer, Integer> getPacketId2Count() {
        return packetId2Count;
    }
}
