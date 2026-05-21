CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE problems (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(32) NOT NULL,
    source VARCHAR(255),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE problem_tags (
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag VARCHAR(64) NOT NULL,
    PRIMARY KEY (problem_id, tag)
);

CREATE TABLE passed_problems (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    passed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_passed_problem UNIQUE (user_id, problem_id)
);

CREATE TABLE problem_sets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE problem_set_items (
    id BIGSERIAL PRIMARY KEY,
    problem_set_id BIGINT NOT NULL REFERENCES problem_sets(id) ON DELETE CASCADE,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_problem_set_item UNIQUE (problem_set_id, problem_id)
);

CREATE INDEX idx_problems_difficulty ON problems(difficulty);
CREATE INDEX idx_problem_tags_tag ON problem_tags(tag);
CREATE INDEX idx_problem_sets_owner ON problem_sets(owner_id);

