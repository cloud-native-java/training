package com.example.oauth2client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
public class Oauth2ClientApplication {
/*
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
	}*/

		@Bean
		RestTemplate restTemplate(OAuth2AuthorizedClientService clientService) {
				return new RestTemplateBuilder()
					.interceptors((ClientHttpRequestInterceptor) (httpRequest, bytes, execution) -> {

							OAuth2AuthenticationToken token = OAuth2AuthenticationToken.class.cast(
								SecurityContextHolder.getContext().getAuthentication());

							OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
								token.getAuthorizedClientRegistrationId(),
								token.getName());

							httpRequest.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken().getTokenValue());

							return execution.execute(httpRequest, bytes);
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
		private final OAuth2AuthorizedClientService clientService;

		ProfileRestController(RestTemplate restTemplate, OAuth2AuthorizedClientService clientService) {
				this.restTemplate = restTemplate;
				this.clientService = clientService;
		}

		@GetMapping("/")
		PrincipalDetails profile(OAuth2AuthenticationToken token) {
				OAuth2AuthorizedClient client = this.clientService
					.loadAuthorizedClient(
						token.getAuthorizedClientRegistrationId(),
						token.getName());
				String uri = client.getClientRegistration()
					.getProviderDetails()
					.getUserInfoEndpoint()
					.getUri();
				ResponseEntity<PrincipalDetails> responseEntity = this.restTemplate
					.exchange(uri, HttpMethod.GET, null, PrincipalDetails.class);
				return responseEntity.getBody();

		}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class PrincipalDetails {
		private String name;
}