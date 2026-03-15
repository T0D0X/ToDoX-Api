CREATE TABLE IF NOT EXISTS users (
    user_id UUID NOT NULL,
    login VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(100) NOT NULL
);

CREATE INDEX idx_users_user_id on users(user_id);
CREATE INDEX idx_users_login on users(login);