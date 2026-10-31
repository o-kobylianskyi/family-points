-- Emails are contact/recovery channels and may be shared by several family accounts.
-- Account usernames and Google provider subjects remain unique identities.
DROP INDEX IF EXISTS uq_user_accounts_email_lower;
CREATE INDEX IF NOT EXISTS idx_user_accounts_email_lower ON user_accounts (lower(email))
WHERE email IS NOT NULL;
