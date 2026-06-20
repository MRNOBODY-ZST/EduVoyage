package cn.edu.shmtu.eduvoyage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("cn.edu.shmtu.eduvoyage")
public class EduvoyageApplication {

	public static void main(String[] args) {
		SpringApplication.run(EduvoyageApplication.class, args);
	}

}
