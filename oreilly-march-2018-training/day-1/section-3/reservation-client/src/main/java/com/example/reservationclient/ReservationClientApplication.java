package com.example.reservationclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@SpringBootApplication
@EnableBinding(Source.class)
@EnableCircuitBreaker
public class ReservationClientApplication {

	@Bean
	RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(5, 7);
	}

	@Bean
	ReactiveUserDetailsService authentication() {
		return new MapReactiveUserDetailsService(
				User.withDefaultPasswordEncoder()
						.username("user")
						.password("password")
						.roles("USER")
						.build()
		);
	}

	@Bean
	WebClient webClient(LoadBalancerExchangeFilterFunction eff) {
		return WebClient
				.builder()
				.filter(eff)
				.build();
	}

	@Bean
	RouterFunction<ServerResponse> routes(WebClient client,
	                                      Source src) {
		return route(GET("/reservations/names"), req -> {

			Flux<String> names = client
					.get()
					.uri("http://reservation-service/reservations")
					.retrieve()
					.bodyToFlux(Reservation.class)
					.map(Reservation::getReservationName);

			Publisher<String> fallback = HystrixCommands
					.from(names)
					.commandName("reservation-names")
					.fallback(Flux.just("EEK!"))
					.eager()
					.build();

			return ServerResponse.ok().body(fallback, String.class);
		})
				.andRoute(POST("/reservations"), req -> {
							Flux<Boolean> sendResult = req.bodyToFlux(Reservation.class)
									.map(Reservation::getReservationName)
									.map(r -> MessageBuilder.withPayload(r).build())
									.map(msg -> src.output().send(msg));
							return ServerResponse.ok().body(sendResult, Boolean.class);
						}
				);
	}

	@Bean
	SecurityWebFilterChain authorization(ServerHttpSecurity security) {
		//@formatter:off
		return
				security
				.csrf().disable()
				.httpBasic()
				.and()
				.authorizeExchange()
					.pathMatchers("/proxy").authenticated()
					.anyExchange().permitAll()
				.and()
				.build();
		//@formatter:on
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb,
	                     RedisRateLimiter rl) {
		return rlb
				.routes()
				.route(rs -> rs
						.path("/proxy")
						.filters(fs -> fs
								.requestRateLimiter(c -> c.setRateLimiter(rl))
								.setPath("/reservations"))
						.uri("lb://reservation-service/"))
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Reservation {

	private String id;

	private String reservationName;
}