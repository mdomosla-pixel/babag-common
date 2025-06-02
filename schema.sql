CREATE TABLE IF NOT EXISTS babag_statuses(
    id TEXT PRIMARY KEY,
    sms_user TEXT,
    from_name TEXT,
    to_number TEXT,
    provider TEXT,
    status TEXT,
    insert_date TIMESTAMP,
    priority INT DEFAULT 1
);

CREATE INDEX ON babag_statuses (status);
