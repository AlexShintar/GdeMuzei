package ru.gdemuzei;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongock
@EnableMongoRepositories
@SpringBootApplication
public class MuseumApp {

	public static void main(String[] args) {
		SpringApplication.run(MuseumApp.class, args);
	}
}
