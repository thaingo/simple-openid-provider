package io.github.vpavic.op.config;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.ServletUtils;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.vpavic.op.key.KeyService;

public class BearerAccessTokenAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(BearerAccessTokenAuthenticationFilter.class);

	private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();

	private final JWSVerifierFactory jwsVerifierFactory = new DefaultJWSVerifierFactory();

	private final KeyService keyService;

	private final AuthenticationManager authenticationManager;

	public BearerAccessTokenAuthenticationFilter(KeyService keyService, AuthenticationManager authenticationManager) {
		this.keyService = Objects.requireNonNull(keyService);
		this.authenticationManager = Objects.requireNonNull(authenticationManager);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		HTTPRequest httpRequest = ServletUtils.createHTTPRequest(request);

		try {
			BearerAccessToken accessToken = BearerAccessToken.parse(httpRequest);
			SignedJWT jwt = SignedJWT.parse(accessToken.getValue());
			JWSHeader header = jwt.getHeader();
			RSAKey rsaKey = (RSAKey) this.keyService.findByKeyId(header.getKeyID());
			JWSVerifier verifier = this.jwsVerifierFactory.createJWSVerifier(header, rsaKey.toPublicKey());
			if (jwt.verify(verifier)) {
				String username = jwt.getJWTClaimsSet().getSubject();
				PreAuthenticatedAuthenticationToken authToken = new PreAuthenticatedAuthenticationToken(username, "");
				authToken.setDetails(this.authenticationDetailsSource.buildDetails(request));
				Authentication authResult = this.authenticationManager.authenticate(authToken);
				SecurityContextHolder.getContext().setAuthentication(authResult);
			}
		}
		catch (Exception e) {
			logger.error("Bearer authentication attempt failed", e);
		}

		filterChain.doFilter(request, response);
	}

}