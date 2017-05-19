package com.sung.elasticsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories("com.sung.elasticsearch.repository")
public class SpringBootElastcsearchExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootElastcsearchExampleApplication.class, args);
	}
}
