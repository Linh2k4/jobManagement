package com.company.jobmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobManagementApplication.class, args);
    }

}
