create table short_links (
    id bigserial primary key,
    code varchar(8) not null unique,
    original_url varchar(2048) not null,
    active boolean not null default true,
    click_count bigint not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    expires_at timestamptz not null,
    user_id bigint not null references users (id) on delete cascade
);

create index ix_short_links_code on short_links (code);
create index ix_short_links_user_id on short_links (user_id);
create index ix_short_links_active_expires_at on short_links (active, expires_at);
