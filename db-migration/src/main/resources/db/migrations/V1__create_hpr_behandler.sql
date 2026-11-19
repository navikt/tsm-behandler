create table hpr_behandler
(
    hpr_nummer     text        not null primary key,
    fnr            text        not null,
    sist_oppdatert timestamp   not null,
    data           jsonb       not null,
    suspendert     boolean     not null,
    oppdatert      timestamptz not null default now()
);

create index hpr_behandler_oppdatert_idx on hpr_behandler (oppdatert);

create index hpr_behandler_fnr_idx on hpr_behandler (fnr);
