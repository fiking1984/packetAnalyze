package demo3;

import demo3.model.DataInfo;
import demo3.utils.UrlUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {

    private Map<Integer, Integer> packetId2Count = new TreeMap<>();

    public static void main(String[] args) {
        int port = 99103;
        String datePattern = null;
        long t1 = System.currentTimeMillis();
        Main main = new Main();
        main.doAnalyze(port, datePattern);
        System.out.println("总耗时: " +(System.currentTimeMillis() - t1) / 1000);
    }

    private void doAnalyze(int port, String datePattern) {
        List<String> urlList = getUrlList(port, datePattern);
        int size = urlList.size();
        List<DataInfo> dataInfoList = new ArrayList<>(size);
        for (String url : urlList) {
            dataInfoList.add(new DataInfo(url, datePattern));
        }
        CompletableFuture[] futureList = new CompletableFuture[size];
        int index = 0;
        try {
            for (DataInfo dataInfo : dataInfoList) {
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(dataInfo::doAnalyze);
            futureList[index++] = future;
        }
            CompletableFuture<Void> completableFuture = CompletableFuture.allOf(futureList);
            completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        //等待所有子任务完成解析后 开始
        dataInfoList.forEach(this::record);
        writeRecord(datePattern);
    }

    private List<String> getUrlList(int port, String datePattern) {
        List<String> list = new ArrayList<>();
        List<String> packets = UrlUtils.getPacket(port, datePattern);
        packets.forEach(str -> list.add(UrlUtils.createUrlByPortAndDate(port, datePattern) + "/packet.log." + str));
        return list;
    }

    private void record(DataInfo dataInfo) {
        dataInfo.getPacketId2Count().forEach((key, value) -> {
            if (packetId2Count.containsKey(key)) {
                packetId2Count.put(key, value + packetId2Count.get(key));
            } else {
                packetId2Count.put(key, value);
            }
        });
        dataInfo.clear();
        dataInfo = null;
    }

    private void writeRecord(String datePattern) {
        String rootPath = System.getProperty("user.dir");
        String fileName = UrlUtils.getDate(datePattern) + "_record.txt";
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

    private String getInfo(StringBuilder sb, int key, int value) {
        sb.append("packetId:").append(key).append(",").append("count:").append(value);
        return sb.toString();
    }
}
