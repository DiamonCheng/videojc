package com.dc.videojc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolCoreSize);
        executor.setMaxPoolSize(poolMaxSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("vediojc-task");
        executor.setThreadGroupName("vediojc-group");
        return executor;
    }
}
