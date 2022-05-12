--- påminner første 100 per tilstand først, slik at vi kan
--- verifisere om ting funker sånn høvelig

update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '5 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '5 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '5 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_HISTORIKK'
    order by id
    limit 100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '5 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '5 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_UFERDIG'
    order by id
    limit 100
);

--- påminner resten av røkla 2 timer etter de første 1000

update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '120 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 100
);

update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '130 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 5100
);

update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '140 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 10100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '150 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 15100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '160 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 20100
);




update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '170 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 5000 offset 100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '180 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 5000 offset 5100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '190 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 5000 offset 10100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '200 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 5000 offset 15100
);



update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '210 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '220 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 5100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '230 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 10100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '240 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 15100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '250 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 20100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '260 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 25100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '270 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 30100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '280 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 35100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '290 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 40100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '300 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 45100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '310 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 50100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '320 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 55100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '330 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 60100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '340 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 65100
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '350 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 70100
);