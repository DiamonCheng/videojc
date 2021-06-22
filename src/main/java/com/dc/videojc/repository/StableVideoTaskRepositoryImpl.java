package com.dc.videojc.repository;

import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriteConfig;
import cn.hutool.core.text.csv.CsvWriter;
import com.dc.videojc.model.SVideoInfo;
import com.dc.videojc.model.VideoInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/22
 */
@Service
public class StableVideoTaskRepositoryImpl implements StableVideoTaskRepository {
    @Value("${vediojc.stable.file-path:./videojc.stable.tsv}")
    private String filePath;
    private Collection<SVideoInfo> data;
    @Autowired
    private ObjectMapper objectMapper;
    
    @PostConstruct
    private void load() throws IOException {
        Path filePathPath = FileSystems.getDefault().getPath(filePath);
        if (Files.isReadable(filePathPath)) {
            CsvReader reader = CsvUtil.getReader();
            reader.setFieldSeparator('\t');
            try (Reader bufferedReader = Files.newBufferedReader(filePathPath, StandardCharsets.UTF_8)) {
                List<?> list = reader.readMapList(bufferedReader);
                data = objectMapper.convertValue(list, new TypeReference<ConcurrentLinkedQueue<SVideoInfo>>() {
                });
            }
        } else {
            data = new ConcurrentLinkedQueue<>();
        }
    }
    
    @PreDestroy
    public void write() throws IOException {
        Path filePathPath = FileSystems.getDefault().getPath(filePath);
        CsvWriteConfig csvWriteConfig = new CsvWriteConfig();
        csvWriteConfig.setFieldSeparator('\t');
        try (Writer bufferedWriter = Files.newBufferedWriter(
                filePathPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE
        );
             CsvWriter csvWriter = new CsvWriter(bufferedWriter, csvWriteConfig)) {
            
            csvWriter.writeLine("id", "source", "ffmpeg", "targetFormat");
            data.stream().map(e -> new String[]{e.getId(), e.getSource(), String.valueOf(e.getFfmpeg()), e.getTargetFormat()}).forEach(csvWriter::writeLine);
        }
    }
    
    @Override
    public void flush() {
        try {
            write();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public void insert(SVideoInfo videoInfo) {
        data.add(videoInfo);
        try {
            write();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public void delete(String id) {
        data.removeIf(e -> Objects.equals(e.getId(), id));
        try {
            write();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public List<VideoInfo> list() {
        return new ArrayList<>(data);
    }
}
