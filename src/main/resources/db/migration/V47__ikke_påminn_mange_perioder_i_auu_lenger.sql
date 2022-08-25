
update paminnelse set neste_paminnelsetidspunkt = TIMESTAMP '9999-01-01 00:00:00'
where id in (
    select id
    from paminnelse
    where tilstand='AVSLUTTET_UTEN_UTBETALING' and endringstidspunkt > TIMESTAMP '2021-12-01 00:00:00'
);
