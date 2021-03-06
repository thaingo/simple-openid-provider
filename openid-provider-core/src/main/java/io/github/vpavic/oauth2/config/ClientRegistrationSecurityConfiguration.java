package io.github.vpavic.oauth2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import io.github.vpavic.oauth2.endpoint.ClientRegistrationEndpoint;

@Order(-2)
@Configuration
public class ClientRegistrationSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.requestMatchers()
				.antMatchers(ClientRegistrationEndpoint.PATH_MAPPING, ClientRegistrationEndpoint.PATH_MAPPING + "/**")
				.and()
			.authorizeRequests()
				.anyRequest().permitAll()
				.and()
			.csrf()
				.disable()
			.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		// @formatter:on
	}

}
