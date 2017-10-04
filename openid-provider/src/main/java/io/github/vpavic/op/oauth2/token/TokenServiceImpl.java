package io.github.vpavic.op.oauth2.token;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.claims.AccessTokenHash;
import com.nimbusds.openid.connect.sdk.claims.AuthorizedParty;
import com.nimbusds.openid.connect.sdk.claims.CodeHash;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.SessionID;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.springframework.stereotype.Service;

import io.github.vpavic.op.config.OpenIdProviderProperties;
import io.github.vpavic.op.oauth2.jwk.JwkSetService;
import io.github.vpavic.op.oauth2.userinfo.UserInfoMapper;

@Service
public class TokenServiceImpl implements TokenService {

	private static final String SCOPE_CLAIM = "scope";

	private static final JWSAlgorithm jwsAlgorithm = JWSAlgorithm.RS256;

	private final OpenIdProviderProperties properties;

	private final JwkSetService jwkSetService;

	private final RefreshTokenStore refreshTokenStore;

	public TokenServiceImpl(OpenIdProviderProperties properties, JwkSetService jwkSetService,
			RefreshTokenStore refreshTokenStore) {
		Objects.requireNonNull(properties, "properties must not be null");
		Objects.requireNonNull(jwkSetService, "jwkSetService must not be null");
		Objects.requireNonNull(refreshTokenStore, "refreshTokenStore must not be null");

		this.properties = properties;
		this.jwkSetService = jwkSetService;
		this.refreshTokenStore = refreshTokenStore;
	}

	@Override
	public AccessToken createAccessToken(AccessTokenRequest accessTokenRequest) {
		Instant issuedAt = Instant.now();
		int tokenLifetime = this.properties.getAccessToken().getLifetime();

		JWK jwk = resolveJwk(JWSAlgorithm.RS256);

		// @formatter:off
		JWSHeader header = new JWSHeader.Builder(jwsAlgorithm)
				.keyID(jwk.getKeyID())
				.build();
		// @formatter:on

		String principal = accessTokenRequest.getPrincipal();
		Scope scope = accessTokenRequest.getScope();

		List<String> audience = new ArrayList<>();
		audience.add(this.properties.getIssuer());

		for (Scope.Value value : scope) {
			String resource = this.properties.getAuthorization().getResourceScopes().get(value.getValue());

			if (resource != null) {
				audience.add(resource);
			}
		}

		// @formatter:off
		JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder()
				.issuer(this.properties.getIssuer())
				.subject(principal)
				.audience(audience)
				.expirationTime(Date.from(issuedAt.plusSeconds(tokenLifetime)))
				.issueTime(Date.from(issuedAt))
				.jwtID(UUID.randomUUID().toString())
				.claim(SCOPE_CLAIM, scope.toString());
		// @formatter:on

		AccessTokenClaimsMapper accessTokenClaimsMapper = accessTokenRequest.getAccessTokenClaimsMapper();

		if (accessTokenClaimsMapper != null) {
			Map<String, Object> claims = accessTokenClaimsMapper.map(principal);
			claims.forEach(claimsSetBuilder::claim);
		}

		JWTClaimsSet claimsSet = claimsSetBuilder.build();

		try {
			SignedJWT accessToken = new SignedJWT(header, claimsSet);
			RSASSASigner signer = new RSASSASigner((RSAKey) jwk);
			accessToken.sign(signer);

			return new BearerAccessToken(accessToken.serialize(), tokenLifetime, scope);
		}
		catch (JOSEException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RefreshToken createRefreshToken(RefreshTokenRequest refreshTokenRequest) {
		Scope scope = refreshTokenRequest.getScope();

		if (!scope.contains(OIDCScopeValue.OFFLINE_ACCESS)) {
			throw new IllegalArgumentException("Scope '" + OIDCScopeValue.OFFLINE_ACCESS + "' is required");
		}

		Instant issuedAt = Instant.now();
		int tokenLifetime = this.properties.getRefreshToken().getLifetime();

		RefreshToken refreshToken = new RefreshToken();
		Instant expiry = (tokenLifetime > 0) ? issuedAt.plusSeconds(tokenLifetime) : null;
		RefreshTokenContext context = new RefreshTokenContext(refreshTokenRequest.getPrincipal(),
				refreshTokenRequest.getClientID(), scope, expiry);
		this.refreshTokenStore.save(refreshToken, context);

		return refreshToken;
	}

	@Override
	public JWT createIdToken(IdTokenRequest idTokenRequest) {
		Scope scope = idTokenRequest.getScope();

		if (!scope.contains(OIDCScopeValue.OPENID)) {
			throw new IllegalArgumentException("Scope '" + OIDCScopeValue.OPENID + "' is required");
		}

		Instant issuedAt = Instant.now();

		JWK jwk = resolveJwk(JWSAlgorithm.RS256);

		// @formatter:off
		JWSHeader header = new JWSHeader.Builder(jwsAlgorithm)
				.keyID(jwk.getKeyID())
				.build();
		// @formatter:on

		String principal = idTokenRequest.getPrincipal();
		ClientID clientID = idTokenRequest.getClientID();

		IDTokenClaimsSet claimsSet = new IDTokenClaimsSet(new Issuer(this.properties.getIssuer()),
				new Subject(principal), Audience.create(clientID.getValue()),
				Date.from(issuedAt.plusSeconds(this.properties.getIdToken().getLifetime())), Date.from(issuedAt));

		claimsSet.setAuthenticationTime(Date.from(idTokenRequest.getAuthenticationTime()));
		claimsSet.setNonce(idTokenRequest.getNonce());
		claimsSet.setACR(idTokenRequest.getAcr());
		claimsSet.setAMR(Collections.singletonList(idTokenRequest.getAmr()));
		claimsSet.setAuthorizedParty(new AuthorizedParty(clientID.getValue()));

		if (this.properties.getFrontChannelLogout().isEnabled()) {
			claimsSet.setSessionID(new SessionID(idTokenRequest.getSessionId()));
		}

		AccessToken accessToken = idTokenRequest.getAccessToken();

		if (accessToken != null) {
			claimsSet.setAccessTokenHash(AccessTokenHash.compute(accessToken, jwsAlgorithm));
		}

		AuthorizationCode code = idTokenRequest.getCode();

		if (code != null) {
			claimsSet.setCodeHash(CodeHash.compute(code, jwsAlgorithm));
		}

		IdTokenClaimsMapper idTokenClaimsMapper = idTokenRequest.getIdTokenClaimsMapper();

		if (idTokenClaimsMapper != null) {
			Map<String, Object> claims = idTokenClaimsMapper.map(principal);
			claims.forEach(claimsSet::setClaim);
		}

		UserInfoMapper userInfoMapper = idTokenRequest.getUserInfoMapper();

		if (userInfoMapper != null) {
			UserInfo userInfo = userInfoMapper.map(principal, scope);
			userInfo.toJSONObject().forEach(claimsSet::setClaim);
		}

		try {
			SignedJWT idToken = new SignedJWT(header, claimsSet.toJWTClaimsSet());
			RSASSASigner signer = new RSASSASigner((RSAKey) jwk);
			idToken.sign(signer);

			return idToken;
		}
		catch (ParseException | JOSEException e) {
			throw new RuntimeException(e);
		}
	}

	private JWK resolveJwk(JWSAlgorithm algorithm) {
		// @formatter:off
		JWKMatcher jwkMatcher = new JWKMatcher.Builder()
				.keyType(KeyType.forAlgorithm(algorithm))
				.keyUse(KeyUse.SIGNATURE)
				.build();
		// @formatter:on

		JWKSelector jwkSelector = new JWKSelector(jwkMatcher);
		List<JWK> jwks;

		try {
			jwks = this.jwkSetService.get(jwkSelector, null);
		}
		catch (KeySourceException e) {
			throw new RuntimeException(e);
		}

		return jwks.iterator().next();
	}

}
