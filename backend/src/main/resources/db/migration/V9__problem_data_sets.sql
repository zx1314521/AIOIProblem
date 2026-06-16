CREATE TABLE problem_data_sets (
    id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT NOT NULL UNIQUE REFERENCES problems(id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL,
    std_cpp TEXT,
    config_yaml TEXT,
    error_message TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE problem_data_cases (
    id BIGSERIAL PRIMARY KEY,
    data_set_id BIGINT NOT NULL REFERENCES problem_data_sets(id) ON DELETE CASCADE,
    case_index INT NOT NULL,
    input TEXT NOT NULL,
    output TEXT NOT NULL,
    CONSTRAINT uq_problem_data_case UNIQUE (data_set_id, case_index)
);

CREATE INDEX idx_problem_data_sets_problem ON problem_data_sets(problem_id);
CREATE INDEX idx_problem_data_cases_set ON problem_data_cases(data_set_id);
