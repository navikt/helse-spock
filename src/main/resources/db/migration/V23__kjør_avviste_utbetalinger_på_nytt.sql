UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN (
    '4a69ae01-25ea-4d1b-9ced-da81a9d5743f',
    '074f1634-08e1-430b-b4c0-523399f8676a'
);