
update paminnelse
set neste_paminnelsetidspunkt = '2022-08-29 17:00:00'
where id in (select id
             from paminnelse
             where tilstand = 'AVSLUTTET_UTEN_UTBETALING'
               and endringstidspunkt > TIMESTAMP '2021-12-01 00:00:00'
               and endringstidspunkt < TIMESTAMP '2022-08-29 00:00:00'
               and neste_paminnelsetidspunkt < TIMESTAMP '2022-08-29 17:00:00'
);
