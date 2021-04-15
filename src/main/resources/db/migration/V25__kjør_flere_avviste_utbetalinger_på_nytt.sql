UPDATE utbetaling
SET neste_paminnelsetidspunkt=now()
WHERE id IN
      (
       '7b81e473-1c83-4360-80e7-36ca5e1d71c6',
       '8b2a088b-726b-4438-8b90-567ba6149a28'
          );