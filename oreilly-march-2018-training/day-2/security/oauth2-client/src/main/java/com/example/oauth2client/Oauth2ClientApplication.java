package com.example.oauth2client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Map;

@SpringBootApplication
@RestController
public class Oauth2ClientApplication {

	@Bean
	@RequestScope
	OAuth2AuthenticationToken token() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return OAuth2AuthenticationToken.class.cast(authentication);
	}

	@Bean
	@RequestScope
	OAuth2AuthorizedClient authorizedClient(
			OAuth2AuthorizedClientService authorizedClientService,
			OAuth2AuthenticationToken oauthToken) {
		return authorizedClientService.loadAuthorizedClient(
				oauthToken.getAuthorizedClientRegistrationId(),
				oauthToken.getName());
	}

	@Bean
	@RequestScope
	RestTemplate buildAuthenticatedRestTemplate(OAuth2AuthorizedClient authorizedClient) {
		return new RestTemplateBuilder()
				.interceptors((ClientHttpRequestInterceptor) (httpRequest, bytes, clientHttpRequestExecution) -> {
					httpRequest.getHeaders().add("Authorization", "Bearer " + authorizedClient.getAccessToken().getTokenValue());
					return clientHttpRequestExecution.execute(httpRequest, bytes);
				})
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(Oauth2ClientApplication.class, args);
	}
}

@RestController
class ProfileRestController {

	private final RestTemplate restTemplate;
	private final OAuth2AuthorizedClient authorizedClient;

	ProfileRestController(RestTemplate restTemplate,
	                      OAuth2AuthorizedClient authorizedClient) {
		this.restTemplate = restTemplate;
		this.authorizedClient = authorizedClient;
	}

	@GetMapping("/profile")
	Map<String, String> profile(OAuth2AuthenticationToken token) {
		String userInfoUri =
				this.authorizedClient
						.getClientRegistration()
						.getProviderDetails()
						.getUserInfoEndpoint()
						.getUri();

		return restTemplate
				.exchange(userInfoUri, HttpMethod.GET,
						null, new ParameterizedTypeReference<Map<String, String>>() {
						})
				.getBody();


	}
}