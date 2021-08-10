UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN (
    '85b5824d-7a60-4c49-b04e-2a845aa63ba7',
    'bbb418b4-31cd-4282-bef4-cbd78ea8de68',
    '5105123e-3637-4197-96a0-d21234fe4ff7',
    'b26ea8d6-8ffe-4f0b-81fc-43a3ac67c857',
    'ca414ec1-e4d4-45a1-807b-ec4fdaf4a9b1',
    '5912059f-5f7c-4ac2-93a3-6f25a17ee1e6',
    'cdb5dc0b-ccf8-4bbb-8a81-19c6beefb66f',
    'c043b61c-90de-47ef-9995-d7be29229369',
    '0147813a-ed92-495d-8a1e-79f198507988'
);
