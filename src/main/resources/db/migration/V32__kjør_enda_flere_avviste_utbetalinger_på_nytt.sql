UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN (
             '8488337c-b788-4947-b0c7-e37e07672b4f',
             '0fc83ea1-7ab2-406b-8b9c-cc0379951406',
             'ee7fac11-12df-4599-94e4-dd10eb7fb928'
    );