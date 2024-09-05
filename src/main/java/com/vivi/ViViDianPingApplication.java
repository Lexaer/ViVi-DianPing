package com.vivi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.vivi.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class ViViDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ViViDianPingApplication.class, args);
    }

}
