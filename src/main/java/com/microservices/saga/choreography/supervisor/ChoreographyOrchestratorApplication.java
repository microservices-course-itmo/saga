package com.microservices.saga.choreography.supervisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;



@SpringBootApplication
@EntityScan("com.microservices.saga.choreography.supervisor.domain.entity")
public class ChoreographyOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChoreographyOrchestratorApplication.class, args);
    }

}
