package com.dc.videojc.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Configuration
public class ThreadPoolConfig {
    @Value("${vediojc.task.pool.core.size:4}")
    private Integer poolCoreSize;
    @Value("${vediojc.task.pool.max.size:16}")
    private Integer poolMaxSize;
    
    @Bean
    @Qualifier("defaultTaskPool")
    @Primary
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolCoreSize);
        executor.setMaxPoolSize(poolMaxSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("vediojc");
        executor.setThreadGroupName("vediojc-task");
        return executor;
    }
    
    @Value("${vediojs.process.monitor.trace-log:false}")
    private Boolean traceLog;
    
    @Bean
    @Qualifier("processMonitorTaskPool")
    public ThreadPoolTaskExecutor processMonitorTaskPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(traceLog ? poolCoreSize * 2 : poolCoreSize);
        executor.setMaxPoolSize(traceLog ? poolMaxSize * 2 : poolMaxSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("vediojc");
        executor.setThreadGroupName("vediojc-process-m");
        return executor;
    }
    
}
