/*
    påminner 1268612 perioder i AUU
    vi tar 80000 i arbeidstid fordelt på 16 batcher, en batch i kvarteret
    1188612 perioder gjenstår etter arbeidstid.
    Resten gjør vi i batcher per time
    1188612 i 4 batcher er 297153, som er ca 300K per time
*/

DO
$$
    BEGIN
        FOR counter IN 0..15
            LOOP
                update paminnelse
                set neste_paminnelsetidspunkt = now() + (interval '15 minutes' * counter)
                where id in (select id
                             from paminnelse
                             where tilstand = 'AVSLUTTET_UTEN_UTBETALING'
                               and endringstidspunkt > TIMESTAMP '2021-12-01 00:00:00'
                               and endringstidspunkt < TIMESTAMP '2022-08-29 00:00:00'
                             order by id
                             limit 5000 offset (counter * 5000));
            END LOOP;
    END;
$$;

DO
$$
    BEGIN
        FOR counter IN 0..3
            LOOP
                update paminnelse
                set neste_paminnelsetidspunkt = TIMESTAMP '2022-08-29 17:00:00' + (interval '60 minutes' * counter)
                where id in (select id
                             from paminnelse
                             where tilstand = 'AVSLUTTET_UTEN_UTBETALING'
                               and endringstidspunkt > TIMESTAMP '2021-12-01 00:00:00'
                               and endringstidspunkt < TIMESTAMP '2022-08-29 00:00:00'
                             order by id
                             limit 300000 offset (80000 + (counter * 300000)));
            END LOOP;
    END;
$$;