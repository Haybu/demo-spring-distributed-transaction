package io.agilehandy.txn;

import io.agilehandy.txn.saga.EventChannels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;

@SpringBootApplication
@EnableBinding(EventChannels.class)
public class SagaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SagaApplication.class, args);
	}

}
