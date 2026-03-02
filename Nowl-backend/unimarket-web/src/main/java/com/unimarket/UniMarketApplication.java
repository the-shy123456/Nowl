package com.unimarket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UniMarket主启动类
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan({"com.unimarket.**.**.mapper"})
public class UniMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniMarketApplication.class, args);
        System.out.println("========================================");
        System.out.println("  UniMarket Backend Started Successfully!");
    }
}
