create table IF NOT EXISTS t_info_extract
(
  name VARCHAR(255) not null,
  id INTEGER constraint t_info_extract_pk primary key autoincrement,
    last_extraction Date not null
);

create unique index if not exists t_info_extract_name_uindex on t_info_extract (name);

