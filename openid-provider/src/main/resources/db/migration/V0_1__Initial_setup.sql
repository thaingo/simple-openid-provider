CREATE TABLE clients (
	id VARCHAR(100) PRIMARY KEY,
	issue_date TIMESTAMP NOT NULL,
	metadata TEXT NOT NULL,
	secret VARCHAR(43),
	registration_uri VARCHAR(200),
	access_token VARCHAR(43)
);

CREATE TABLE refresh_tokens (
	token VARCHAR(43) PRIMARY KEY,
	client_id VARCHAR(100) NOT NULL,
	subject VARCHAR(30) NOT NULL,
	scope VARCHAR(200) NOT NULL,
	expiry BIGINT NOT NULL,
	UNIQUE (client_id, subject)
);
