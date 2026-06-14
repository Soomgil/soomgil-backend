create table event_publication (
    id uuid not null,
    publication_date timestamp(6) with time zone not null,
    listener_id varchar(255) not null,
    serialized_event varchar(255) not null,
    event_type varchar(255) not null,
    completion_date timestamp(6) with time zone,
    primary key (id)
);

create table event_publication_archive (
    id uuid not null,
    publication_date timestamp(6) with time zone not null,
    listener_id varchar(255) not null,
    serialized_event varchar(255) not null,
    event_type varchar(255) not null,
    completion_date timestamp(6) with time zone,
    primary key (id)
);
