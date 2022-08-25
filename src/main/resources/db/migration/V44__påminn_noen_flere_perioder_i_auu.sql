--- påminner 1000 perioder i AUU som et dry-run før alle skal påminnes

update paminnelse set neste_paminnelsetidspunkt = now()
where id in (
    select id
    from paminnelse
    where tilstand='AVSLUTTET_UTEN_UTBETALING' and endringstidspunkt > TIMESTAMP '2021-12-01 00:00:00'
    limit 1000
);
