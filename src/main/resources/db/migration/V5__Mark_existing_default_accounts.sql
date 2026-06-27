UPDATE accounts
SET is_default = TRUE
WHERE name IN ('Bank', 'Wallet', 'Cash', 'Savings')
AND is_default = FALSE;