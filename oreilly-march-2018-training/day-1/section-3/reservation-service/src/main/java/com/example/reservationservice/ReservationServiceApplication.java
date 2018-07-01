package com.example.reservationservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@EnableBinding(Sink.class)
@SpringBootApplication
public class ReservationServiceApplication {

	private final ReservationRepository rr;

	public ReservationServiceApplication(ReservationRepository rr) {
		this.rr = rr;
	}

	@StreamListener
	public void incomingMessages(@Input(Sink.INPUT) Flux<String> rnPublisher) {
		rnPublisher
				.map(rn -> new Reservation(null, rn))
				.flatMap(this.rr::save)
				.subscribe(r -> System.out.println("received and saved " + r.getId() + " with " + r.getReservationName() + "."));
	}

	@Bean
	RouterFunction<ServerResponse> routes(ReservationRepository rr,
	                                      Environment environment) {
		return route(GET("/reservations"), req -> ok().body(rr.findAll(), Reservation.class));
	}

	@Bean
	ApplicationRunner run(ReservationRepository rr) {
		return args ->
				rr
						.deleteAll()
						.thenMany(Flux.just("Josh", "Kenny", "Dave", "Madhura", "Tasha", "Mario", "Jennifer", "Tammie")
								.map(name -> new Reservation(null, name))
								.flatMap(rr::save))
						.thenMany(rr.findAll())
						.subscribe(System.out::println);
	}

	@EventListener(RefreshScopeRefreshedEvent.class)
	public void configRefreshed(RefreshScopeRefreshedEvent event) {
		System.out.println("something's changed! reconfiguring internal state now...");
	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}
}

@RefreshScope
@RestController
class MessageRestController {

	private final String value;

	MessageRestController(@Value("${message:default}") String value) {
		this.value = value;
	}

	@GetMapping("/message")
	Publisher<String> msg() {
		return Mono.just(this.value);
	}
}

interface ReservationRepository extends ReactiveMongoRepository<Reservation, String> {
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class Reservation {

	@Id
	private String id;

	private String reservationName;
}