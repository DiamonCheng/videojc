package com.dc.videojc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/21
 */
@Service
@Slf4j
public class StandardProcessMonitor {
    @Value("${vediojs.process.monitor.trace-log:false}")
    private Boolean traceLog;
    
    @Autowired
    @Qualifier("processMonitorTaskPool")
    private ThreadPoolTaskExecutor processMonitorTaskPool;
    
    public void monitor(Process process, String desc) {
        if (traceLog) {
            processMonitorTaskPool.execute(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while (null != (line = reader.readLine())) {
                        log.trace("[{}]:{}", desc, line);
                    }
                } catch (Exception e) {
                    log.error("Monitor Process failed[" + desc + "]", e);
                }
            });
        }
        processMonitorTaskPool.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while (null != (line = reader.readLine())) {
                    log.error("[{}]:{}", desc, line);
                }
            } catch (Exception e) {
                log.error("Monitor Process failed[" + desc + "]", e);
            }
        });
    }
    
}
