create table if not exists t_ext_chrome_urls
(
  id              INTEGER primary key,
  url             LONGVARCHAR,
  title           LONGVARCHAR,
  visit_count     INTEGER default 0 not null,
  typed_count     INTEGER default 0 not null,
  last_visit_time INTEGER not null,
  hidden          INTEGER default 0 not null
);
create index IF NOT EXISTS urls_url_index
  on t_ext_chrome_urls (url);
create table if not exists t_ext_chrome_visits
(
  id                              INTEGER primary key,
  url                             INTEGER not null,
  visit_time                      INTEGER not null,
  from_visit                      INTEGER,
  transition                      INTEGER default 0 not null,
  segment_id                      INTEGER,
  visit_duration                  INTEGER default 0 not null,
  incremented_omnibox_typed_score BOOLEAN default FALSE not null
);
create index  if not exists  visits_from_index
  on t_ext_chrome_visits (from_visit);
create index  if not exists  visits_time_index
  on t_ext_chrome_visits (visit_time);
create index if not exists  visits_url_index
  on t_ext_chrome_visits (url);

CREATE TABLE if not exists t_ext_chrome_login_data
(
  origin_url            VARCHAR,
  action_url            VARCHAR,
  username_element      VARCHAR,
  username_value        VARCHAR,
  password_element      VARCHAR,
  password_value        BLOB,
  submit_element        VARCHAR,
  signon_realm          VARCHAR,
  preferred             INTEGER,
  date_created          INTEGER,
  blacklisted_by_user   INTEGER,
  scheme                INTEGER,
  password_type         INTEGER,
  times_used            INTEGER,
  form_data             BLOB,
  date_synced           INTEGER,
  display_name          VARCHAR,
  icon_url              VARCHAR,
  federation_url        VARCHAR,
  skip_zero_click       INTEGER,
  generation_upload_status INTEGER,
  possible_username_pairs BLOB,
  id                    INTEGER
);

CREATE TABLE if not exists t_ext_chrome_downloads
(
  id                   INTEGER PRIMARY KEY,
  guid                 VARCHAR,
  current_path         LONGVARCHAR,
  target_path          LONGVARCHAR,
  start_time           INTEGER,
  received_bytes       INTEGER,
  total_bytes          INTEGER,
  state                INTEGER,
  danger_type          INTEGER,
  interrupt_reason     INTEGER,
  hash                 BLOB,
  end_time             INTEGER,
  opened               INTEGER,
  referrer             VARCHAR,
  site_url             VARCHAR,
  tab_url              VARCHAR,
  tab_referrer_url     VARCHAR,
  http_method          VARCHAR,
  by_ext_id            VARCHAR,
  by_ext_name          VARCHAR,
  etag                 VARCHAR,
  last_modified        VARCHAR,
  mime_type            VARCHAR(255),
  original_mime_type   VARCHAR(255),
  last_access_time     INTEGER DEFAULT 0,
  transient            INTEGER DEFAULT 0
);

CREATE TABLE if not exists t_ext_chrome_downloads_slices
(
  download_id          INTEGER,
  offset               INTEGER,
  received_bytes       INTEGER,
  finished             INTEGER DEFAULT 0,
  PRIMARY KEY (download_id, offset)
);

CREATE TABLE if not exists t_ext_chrome_downloads_url_chains
(
  id                   INTEGER ,
  chain_index          INTEGER ,
  url                  LONGVARCHAR ,
  PRIMARY KEY (id, chain_index)
);

CREATE TABLE if not exists t_ext_chrome_keyword_search_terms
(
  keyword_id           INTEGER ,
  url_id               INTEGER ,
  lower_term           LONGVARCHAR ,
  term                 LONGVARCHAR
);



CREATE TABLE if not exists t_ext_chrome_segment_usage
(
  id                   INTEGER PRIMARY KEY,
  segment_id           INTEGER ,
  time_slot            INTEGER ,
  visit_count          INTEGER DEFAULT 0
);

CREATE TABLE if not exists t_ext_chrome_segments
(
  id                   INTEGER PRIMARY KEY,
  name                 VARCHAR,
  url_id               INTEGER NOT NULL
);


CREATE TABLE if not exists t_ext_chrome_typed_url_sync_metadata
(
  storage_key          INTEGER PRIMARY KEY ,
  value                BLOB
);

CREATE TABLE if not exists t_ext_chrome_visit_source
(
  id                   INTEGER PRIMARY KEY,
  source               INTEGER
);

CREATE INDEX if not exists keyword_search_terms_index1
  ON t_ext_chrome_keyword_search_terms (keyword_id, lower_term);

CREATE INDEX if not exists keyword_search_terms_index2
  ON t_ext_chrome_keyword_search_terms (url_id);

CREATE INDEX if not exists keyword_search_terms_index3
  ON t_ext_chrome_keyword_search_terms (term);

CREATE INDEX if not exists segment_usage_time_slot_segment_id
  ON t_ext_chrome_segment_usage(time_slot, segment_id);

CREATE INDEX if not exists segments_name
  ON t_ext_chrome_segments(name);

CREATE INDEX if not exists segments_url_id
  ON t_ext_chrome_segments(url_id);

CREATE INDEX if not exists segments_usage_seg_id
  ON t_ext_chrome_segment_usage(segment_id);

CREATE INDEX if not exists urls_url_index
  ON t_ext_chrome_urls (url);

CREATE INDEX if not exists visits_time_index
  ON t_ext_chrome_visits (visit_time);

CREATE INDEX if not exists visits_url_index
  ON t_ext_chrome_visits (url);
