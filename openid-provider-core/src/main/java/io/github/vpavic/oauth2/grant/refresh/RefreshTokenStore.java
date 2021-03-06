package io.github.vpavic.oauth2.grant.refresh;

import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

public interface RefreshTokenStore {

	void save(RefreshTokenContext context);

	RefreshTokenContext load(RefreshToken refreshToken) throws GeneralException;

	RefreshTokenContext findByClientIdAndSubject(ClientID clientId, Subject subject);

	void revoke(RefreshToken refreshToken);

}
