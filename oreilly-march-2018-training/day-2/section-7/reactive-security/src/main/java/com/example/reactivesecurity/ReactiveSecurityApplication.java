package com.example.reactivesecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class ReactiveSecurityApplication {

	@Bean
	RouterFunction<ServerResponse> routes() {
		return RouterFunctions
				.route(GET("/greeting"),
						request ->
								ok().body(
										request
												.principal()
												.map(Principal::getName)
												.map(name -> "Hello, " + name + "!"), String.class))
				.andRoute(GET("/hi/{name}"), req -> ok().body(Flux.just("Hello, " + req.pathVariable("name") + "!"), String.class));
	}

	@Bean
	ReactiveUserDetailsService authentication() {
		return new MapReactiveUserDetailsService(
				User.withDefaultPasswordEncoder()
						.roles("USER")
						.username("user")
						.password("password")
						.build());
	}

	@Bean
	SecurityWebFilterChain authorization(ServerHttpSecurity http) {
		ReactiveAuthorizationManager<AuthorizationContext> auth =
				(authentication, object) -> Mono.just(new AuthorizationDecision(object.getVariables().get("name").equals("rwinch")));

		//@formatter:off
		return
				http
				.authorizeExchange()
					.pathMatchers("/greeting").authenticated()
					.pathMatchers("/hi/{name}").access(auth)
				.and()
					.csrf()
						.disable()
				.httpBasic()
				.and()
				.build();
		//@formatter:on
	}

	public static void main(String[] args) {
		SpringApplication.run(ReactiveSecurityApplication.class, args);
	}
}
