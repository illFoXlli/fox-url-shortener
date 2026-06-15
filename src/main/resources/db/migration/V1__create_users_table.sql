create table users (
    id bigserial primary key,
    username varchar(50) not null unique,
    password_hash varchar(255) not null,
    role varchar(20) not null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index ix_users_username on users (username);
