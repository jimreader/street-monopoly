ALTER TABLE event_player
ADD COLUMN invite_email_status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (invite_email_status IN ('pending', 'sent', 'failed')),
ADD COLUMN invite_email_sent_at TIMESTAMP,
ADD COLUMN invite_email_error VARCHAR(500);

-- Pre-existing invites were sent synchronously before this feature existed, so treat them as delivered.
UPDATE event_player SET invite_email_status = 'sent', invite_email_sent_at = invited_at;
