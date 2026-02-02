-- Streaming Platform Database Schema
-- PostgreSQL 16+
-- Clean Architecture compliant schema

-- ============================================
-- STREAMS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS streams (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    stream_key VARCHAR(16) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('WAITING', 'LIVE', 'ENDED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    current_viewers INTEGER NOT NULL DEFAULT 0,
    viewers_peak INTEGER NOT NULL DEFAULT 0
);

-- Índices para streams
CREATE INDEX IF NOT EXISTS idx_streams_status ON streams(status);
CREATE INDEX IF NOT EXISTS idx_streams_created_at ON streams(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_streams_stream_key ON streams(stream_key);
CREATE INDEX IF NOT EXISTS idx_streams_status_created_at ON streams(status, created_at DESC);

-- ============================================
-- STREAM EVENTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS stream_events (
    id BIGSERIAL PRIMARY KEY,
    stream_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('CREATED', 'STARTED', 'ENDED', 'VIEWER_JOINED', 'VIEWER_LEFT')),
    metadata TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stream_events_stream FOREIGN KEY (stream_id) REFERENCES streams(id) ON DELETE CASCADE
);

-- Índices para stream_events
CREATE INDEX IF NOT EXISTS idx_stream_events_stream_id ON stream_events(stream_id);
CREATE INDEX IF NOT EXISTS idx_stream_events_timestamp ON stream_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_stream_events_stream_event_type ON stream_events(stream_id, event_type);
CREATE INDEX IF NOT EXISTS idx_stream_events_event_type_timestamp ON stream_events(event_type, timestamp DESC);

-- ============================================
-- VIEWER SESSIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS viewer_sessions (
    id BIGSERIAL PRIMARY KEY,
    stream_id UUID NOT NULL,
    viewer_id VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    CONSTRAINT fk_viewer_sessions_stream FOREIGN KEY (stream_id) REFERENCES streams(id) ON DELETE CASCADE
);

-- Índices para viewer_sessions
CREATE INDEX IF NOT EXISTS idx_viewer_sessions_stream_id ON viewer_sessions(stream_id);
CREATE INDEX IF NOT EXISTS idx_viewer_sessions_viewer_id ON viewer_sessions(viewer_id);
CREATE INDEX IF NOT EXISTS idx_viewer_sessions_active ON viewer_sessions(stream_id, viewer_id) WHERE left_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_viewer_sessions_joined_at ON viewer_sessions(joined_at DESC);

-- ============================================
-- COMENTÁRIOS E DOCUMENTAÇÃO
-- ============================================

COMMENT ON TABLE streams IS 'Armazena informações de todas as streams criadas na plataforma';
COMMENT ON COLUMN streams.id IS 'Identificador único da stream (UUID v4)';
COMMENT ON COLUMN streams.stream_key IS 'Chave única de 16 caracteres para autenticação RTMP';
COMMENT ON COLUMN streams.status IS 'Status atual: WAITING (aguardando conexão), LIVE (transmitindo), ENDED (finalizada)';
COMMENT ON COLUMN streams.viewers_peak IS 'Maior número de viewers simultâneos já registrados';

COMMENT ON TABLE stream_events IS 'Registro histórico de todos os eventos relacionados a streams';
COMMENT ON COLUMN stream_events.event_type IS 'Tipo do evento: CREATED, STARTED, ENDED, VIEWER_JOINED, VIEWER_LEFT';
COMMENT ON COLUMN stream_events.metadata IS 'Dados adicionais em formato JSON (opcional)';

COMMENT ON TABLE viewer_sessions IS 'Registra cada sessão de visualização de uma stream';
COMMENT ON COLUMN viewer_sessions.viewer_id IS 'Identificador do viewer (pode ser IP, sessionId, userId)';
COMMENT ON COLUMN viewer_sessions.left_at IS 'Timestamp de saída (NULL se ainda assistindo)';

-- ============================================
-- DADOS DE EXEMPLO (OPCIONAL - APENAS DESENVOLVIMENTO)
-- ============================================

-- INSERT INTO streams (id, title, description, stream_key, status, created_at, current_viewers, viewers_peak)
-- VALUES 
--     (gen_random_uuid(), 'Stream de Teste 1', 'Descrição de exemplo', 'testkey12345abcd', 'WAITING', CURRENT_TIMESTAMP, 0, 0);
