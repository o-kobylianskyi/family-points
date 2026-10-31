CREATE TABLE point_type_name_forms (
    id BIGSERIAL PRIMARY KEY,
    point_type_id BIGINT NOT NULL REFERENCES point_types(id) ON DELETE CASCADE,
    language VARCHAR(5) NOT NULL,
    one VARCHAR(100) NOT NULL,
    few VARCHAR(100) NOT NULL,
    many VARCHAR(100) NOT NULL,
    CONSTRAINT uq_point_type_name_forms_lang UNIQUE (point_type_id, language),
    CONSTRAINT ck_point_type_name_forms_lang CHECK (language IN ('uk', 'ru', 'en', 'de'))
);
