create table if not exists transformer
(
    id                          varchar(37) not null primary key,
    destination                 longtext   null,
    transformer_actions         longtext   null,
    source_submodel_id_rules    longtext   null,
    optlock                     int        null,
    version                     int        null
    );

create table if not exists transformation_log
(
    id                              int             not null auto_increment primary key,
    date                            DATETIME        null,
    destination_aas_id              text            null,
    destination_submodel_id         text            null,
    source_submodel_id              text            null,
    executor_id                     varchar(37)     null,
    transformation_duration_in_ms   long            null,
    source_lookup_in_ms             long            null,
    destination_save_in_ms          long            null
);