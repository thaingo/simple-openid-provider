package io.github.vpavic.oauth2.grant.refresh;

import java.time.Instant;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

/**
 * Collection of utils for refresh token related testing scenarios.
 *
 * @author Vedran Pavic
 */
final class RefreshTokenTestUtils {

	private RefreshTokenTestUtils() {
	}

	static RefreshTokenContext createRefreshTokenContext(Instant expiry) {
		return new RefreshTokenContext(new RefreshToken(), new ClientID(), new Subject("test"), new Scope(), expiry);
	}

}
