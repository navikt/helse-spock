UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN (
             '122987bb-3fa0-4374-88f4-258e2a192030',
             'db110717-c4f1-4f7f-ac24-e252d45003ae',
             'd8f9b7fc-c0d6-4dab-ae88-0286d8c36e8e',
             'b6fd0b32-03a4-4837-a31b-ac28f9a5070f',
             '611216dd-0bd2-40e4-8b83-6542d26bd91f',
             '8727be62-475d-4be0-9c7d-f5e40ca79346',
             '075b4c75-2788-4f1a-941b-2fc3247045ff'
    );