package net.isucon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class SearchNameApplication {
	public static void main(String[] args) throws Exception {
		SpringApplication.run(SearchNameApplication.class, args);
	}

}
