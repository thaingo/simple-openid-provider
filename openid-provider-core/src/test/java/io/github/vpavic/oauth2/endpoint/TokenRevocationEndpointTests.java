package io.github.vpavic.oauth2.endpoint;

import com.nimbusds.oauth2.sdk.id.Issuer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.github.vpavic.oauth2.client.ClientRepository;
import io.github.vpavic.oauth2.config.OpenIdProviderWebMvcConfiguration;
import io.github.vpavic.oauth2.config.TokenSecurityConfiguration;
import io.github.vpavic.oauth2.grant.refresh.RefreshTokenStore;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Tests for {@link TokenRevocationEndpoint}.
 *
 * @author Vedran Pavic
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration
public class TokenRevocationEndpointTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
	}

	@Test
	public void test() {
		// TODO
	}

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	@Import({ OpenIdProviderWebMvcConfiguration.class, TokenSecurityConfiguration.class })
	static class Config {

		@Bean
		public TokenRevocationEndpoint tokenRevocationEndpoint() {
			return new TokenRevocationEndpoint(new Issuer("http://example.com"), mock(ClientRepository.class),
					mock(RefreshTokenStore.class));
		}

	}

}
