-- V32__Add_refresh_token_grace_period.sql
-- Adds support for a grace period during refresh token rotation to prevent race conditions.

ALTER TABLE refresh_tokens ADD COLUMN rotated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE refresh_tokens ADD COLUMN is_used BOOLEAN DEFAULT FALSE;

-- Create an index to help with cleanup of old rotated tokens
CREATE INDEX idx_refresh_tokens_rotated_at ON refresh_tokens(rotated_at);
