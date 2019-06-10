CREATE TABLE news
(
  id UUID PRIMARY KEY NOT NULL,
  title varchar(255) NOT NULL,
  greeting varchar(255) not null default '',
  content_text text NOT NULL DEFAULT '',
  signature varchar(255) not null default '',
  start_view timestamp DEFAULT NOW() NOT NULL,
  end_view timestamp DEFAULT (NOW() + INTERVAL '3 DAY') NOT NULL,
  read_required boolean DEFAULT false  NOT NULL,
  segments integer[] not null default ('{0,1,2,3,4}'::integer[]),
  channels varchar(10)[] not null default ('{ANY}'::text[]),
  inns text[] not null default ('{ANY}'::text[]),
  archive boolean DEFAULT false NOT NULL
);

CREATE INDEX start_view_end_view ON news(start_view desc, end_view desc) where news.archive != true;

create table reading_log(
  news_id UUID references news(id) not null,
  user_id varchar(60) not null,
  fio varchar(60) not null,
  segment integer not null,
  channel varchar(10) not null,
  inn varchar(60) not null,
  org_id varchar(60) not null,
  date timestamp not null default now(),
  unique (news_id, user_id)
);
create table blacklist (id UUID, expire TIMESTAMP, sys_create_time TIMESTAMP default now());


CREATE UNIQUE INDEX blacklist_id ON blacklist(ID);

create type audit_action as enum('create', 'update', 'archive');

CREATE TABLE audit(
  news_id UUID references news(id) not null,
  user_id varchar(20) not null,
  date timestamp not null default now(),
  action audit_action not null
);

