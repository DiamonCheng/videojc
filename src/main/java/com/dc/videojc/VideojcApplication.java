package com.dc.videojc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VideojcApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(VideojcApplication.class, args);
    }
    
}
