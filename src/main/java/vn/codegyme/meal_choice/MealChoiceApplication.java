package vn.codegyme.meal_choice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MealChoiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MealChoiceApplication.class, args);
	}

}
