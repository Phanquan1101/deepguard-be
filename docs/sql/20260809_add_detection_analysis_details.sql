-- Persist the aggregate and per-frame data returned by the detection service.
-- Run once against the production PostgreSQL database before deploying this code.

ALTER TABLE detection_results
    ADD COLUMN IF NOT EXISTS ai_generated_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS not_ai_generated_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS deepfake_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS ai_generated_audio_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS not_ai_generated_audio_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS attributed_generator VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_video BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE detection_frames
    ADD COLUMN IF NOT EXISTS frame_index INTEGER,
    ADD COLUMN IF NOT EXISTS ai_generated_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS not_ai_generated_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS deepfake_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS attributed_generator VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_generated_audio_score NUMERIC(6, 5),
    ADD COLUMN IF NOT EXISTS not_ai_generated_audio_score NUMERIC(6, 5);

CREATE INDEX IF NOT EXISTS idx_detection_frames_result_frame
    ON detection_frames (detect_result_id, frame_index);
