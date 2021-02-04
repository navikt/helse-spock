UPDATE person SET neste_paminnelsetidspunkt = '2021-02-04 18:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 0);

UPDATE person SET neste_paminnelsetidspunkt = '2021-02-04 19:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 100000);

UPDATE person SET neste_paminnelsetidspunkt = '2021-02-04 20:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 200000);

UPDATE person SET neste_paminnelsetidspunkt = '2021-02-04 21:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 300000);

UPDATE person SET neste_paminnelsetidspunkt = '2021-02-04 22:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 400000);

UPDATE person SET neste_paminnelsetidspunkt = '2021-02-04 23:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 500000);

UPDATE person SET neste_paminnelsetidspunkt = '2021-02-05 00:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 600000);

UPDATE person SET neste_paminnelsetidspunkt = '2021-02-05 01:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 700000);

UPDATE person SET neste_paminnelsetidspunkt = '2021-02-05 02:00:00'
    WHERE fnr IN (SELECT fnr FROM person ORDER BY fnr LIMIT 100000 OFFSET 800000);