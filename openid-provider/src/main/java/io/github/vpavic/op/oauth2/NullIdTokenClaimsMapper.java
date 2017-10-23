package io.github.vpavic.op.oauth2;

import java.util.Collections;
import java.util.Map;

import io.github.vpavic.oauth2.token.IdTokenClaimsMapper;

public class NullIdTokenClaimsMapper implements IdTokenClaimsMapper {

	@Override
	public Map<String, Object> map(String principal) {
		return Collections.emptyMap();
	}

}