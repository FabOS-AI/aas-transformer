create table if not exists transformation_description
(
    id                          int auto_increment primary key,
    transformer_id              varchar(37) not null,
    source_submodel_id          text not null,
    target_submodel_id          text not null,
    UNIQUE (transformer_id, source_submodel_id, target_submodel_id)
);