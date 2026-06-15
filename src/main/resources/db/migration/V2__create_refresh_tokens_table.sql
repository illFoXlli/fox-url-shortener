create table refresh_tokens (
    id bigserial primary key,
    token_hash varchar(64) not null unique,
    user_id bigint not null references users (id) on delete cascade,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    user_agent varchar(512),
    ip_address varchar(64)
);

create index ix_refresh_tokens_user_id on refresh_tokens (user_id);
create index ix_refresh_tokens_token_hash on refresh_tokens (token_hash);
