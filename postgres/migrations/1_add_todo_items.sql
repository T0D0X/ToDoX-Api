CREATE TABLE IF NOT EXISTS todo_items (
    user_id UUID NOT NULL,
    id UUID PRIMARY KEY NOT NULL,
    description TEXT DEFAULT NULL,
    priority VARCHAR(20) NOT NULL,
    is_complete BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP(0) WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP(0) WITH TIME ZONE DEFAULT NULL,
    tags TEXT[] DEFAULT '{}',

    CONSTRAINT valid_priority CHECK (priority IN ('low', 'medium', 'high'))
);

CREATE INDEX idx_todo_items_id ON todo_items(id);
CREATE INDEX idx_todo_items_user_id ON todo_items(user_id);