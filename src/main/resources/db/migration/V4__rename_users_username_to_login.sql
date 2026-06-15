alter table users rename column username to login;

alter index ix_users_username rename to ix_users_login;

alter table users rename constraint users_username_key to users_login_key;
