--- påminner alle tilstandene i batch på 5000 stk, fordelt på 2 min mellom hver batch

update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '0 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 0
);

update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '2 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 5000
);

update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '4 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 10000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '6 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 15000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '10 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_BLOKKERENDE_PERIODE'
    order by id
    limit 5000 offset 20000
);




update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '15 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 5000 offset 0
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '20 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 5000 offset 5000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '25 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 5000 offset 10000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '30 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_GODKJENNING'
    order by id
    limit 5000 offset 15000
);



update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '40 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 0
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '50 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 5000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '60 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 10000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '70 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 15000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '80 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 20000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '90 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 25000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '100 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 30000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '110 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 35000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '120 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 40000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '130 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 45000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '140 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 50000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '150 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 55000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '160 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 60000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '170 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 65000
);
update paminnelse set antall_ganger_paminnet=8999, neste_paminnelsetidspunkt = now() + interval '180 minutes'
where id in (
    select id
    from paminnelse
    where tilstand = 'AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK'
    order by id
    limit 5000 offset 70000
);