-- Tabla de partidas
CREATE TABLE IF NOT EXISTS partida (
    id               SERIAL PRIMARY KEY,
    jugador_blancas  VARCHAR(50) NOT NULL,
    jugador_negras   VARCHAR(50) NOT NULL,
    ganador          VARCHAR(10),          -- 'WHITE', 'BLACK', 'DRAW', NULL si en curso
    resultado        VARCHAR(20),          -- 'CHECKMATE', 'STALEMATE', 'RESIGNED'
    total_turnos     INT DEFAULT 0,
    fecha_inicio     TIMESTAMP DEFAULT NOW(),
    fecha_fin        TIMESTAMP
);

-- Tabla de movimientos
CREATE TABLE IF NOT EXISTS movimiento (
    id           SERIAL PRIMARY KEY,
    partida_id   INT REFERENCES partida(id) ON DELETE CASCADE,
    turno        INT NOT NULL,
    color        VARCHAR(10) NOT NULL,     -- 'WHITE' o 'BLACK'
    movimiento   VARCHAR(10) NOT NULL,     -- notación algebraica ej: 'e4', 'Nf3'
    en_jaque     BOOLEAN DEFAULT FALSE,
    fecha        TIMESTAMP DEFAULT NOW()
);
