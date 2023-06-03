package com.example.moyiza_be;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableEncryptableProperties
@EnableCaching
public class MoyizaBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoyizaBeApplication.class, args);
    }

}
