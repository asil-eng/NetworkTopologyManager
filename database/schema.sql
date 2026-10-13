-- =====================================================================
-- Network Topology Manager - MySQL database setup
-- Run this once:   mysql -u root -p < database/schema.sql
-- WARNING: it deletes and recreates the network_manager database.
-- =====================================================================

DROP DATABASE IF EXISTS network_manager;
CREATE DATABASE network_manager;
USE network_manager;

-- Login accounts. Passwords are stored as SHA-256 hashes, never as plain text.
CREATE TABLE users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password CHAR(64)     NOT NULL,
    role     ENUM('ADMIN', 'VIEWER') NOT NULL
);

-- One row per saved network design.
CREATE TABLE networks (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_by  INT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Routers, switches, PCs and servers placed on a network's canvas.
CREATE TABLE devices (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    network_id INT         NOT NULL,
    name       VARCHAR(50) NOT NULL,
    type       ENUM('ROUTER', 'SWITCH', 'PC', 'SERVER') NOT NULL,
    ip_address VARCHAR(15) NOT NULL,
    x          INT         NOT NULL,
    y          INT         NOT NULL,
    online     BOOLEAN     NOT NULL DEFAULT TRUE,
    FOREIGN KEY (network_id) REFERENCES networks(id) ON DELETE CASCADE,
    UNIQUE (network_id, ip_address)
);

-- Cables between two devices. latency_ms is the "weight" used by Dijkstra.
CREATE TABLE connections (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    network_id INT NOT NULL,
    device_a   INT NOT NULL,
    device_b   INT NOT NULL,
    latency_ms INT NOT NULL DEFAULT 10,
    FOREIGN KEY (network_id) REFERENCES networks(id) ON DELETE CASCADE,
    FOREIGN KEY (device_a)   REFERENCES devices(id)  ON DELETE CASCADE,
    FOREIGN KEY (device_b)   REFERENCES devices(id)  ON DELETE CASCADE
);

-- History of every packet sent in the simulator.
CREATE TABLE packet_logs (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    network_id     INT NOT NULL,
    source_id      INT,
    destination_id INT,
    path           VARCHAR(500),
    algorithm      VARCHAR(20),
    hops           INT,
    total_latency  INT,
    status         ENUM('DELIVERED', 'FAILED') NOT NULL,
    sent_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (network_id)     REFERENCES networks(id) ON DELETE CASCADE,
    FOREIGN KEY (source_id)      REFERENCES devices(id)  ON DELETE SET NULL,
    FOREIGN KEY (destination_id) REFERENCES devices(id)  ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
-- Demo data
-- ---------------------------------------------------------------------
INSERT INTO users (username, password, role) VALUES
    ('admin',  SHA2('admin123', 256),  'ADMIN'),
    ('viewer', SHA2('viewer123', 256), 'VIEWER');

INSERT INTO networks (name, description, created_by) VALUES
    ('College Computer Lab', 'Two lab subnets joined by routers, with a backup route through R3', 1);

-- Device ids will be 1..10 because the database is brand new.
INSERT INTO devices (network_id, name, type, ip_address, x, y) VALUES
    (1, 'R1',   'ROUTER', '192.168.1.1',  300, 60),   -- 1
    (1, 'R2',   'ROUTER', '192.168.2.1',  700, 60),   -- 2
    (1, 'R3',   'ROUTER', '10.0.0.1',     500, 200),  -- 3
    (1, 'SW1',  'SWITCH', '192.168.1.2',  200, 260),  -- 4
    (1, 'SW2',  'SWITCH', '192.168.2.2',  800, 260),  -- 5
    (1, 'PC1',  'PC',     '192.168.1.10',  60, 440),  -- 6
    (1, 'PC2',  'PC',     '192.168.1.11', 200, 440),  -- 7
    (1, 'PC3',  'PC',     '192.168.1.12', 340, 440),  -- 8
    (1, 'SRV1', 'SERVER', '192.168.2.10', 720, 440),  -- 9
    (1, 'PC4',  'PC',     '192.168.2.11', 880, 440);  -- 10

-- R1-R2 is ONE hop but slow (50 ms). R1-R3-R2 is TWO hops but fast (20 ms).
-- So BFS (fewest hops) and Dijkstra (lowest latency) choose different routes.
INSERT INTO connections (network_id, device_a, device_b, latency_ms) VALUES
    (1, 1, 2, 50),
    (1, 1, 3, 10),
    (1, 3, 2, 10),
    (1, 1, 4, 5),
    (1, 2, 5, 5),
    (1, 4, 6, 2),
    (1, 4, 7, 2),
    (1, 4, 8, 2),
    (1, 5, 9, 2),
    (1, 5, 10, 2);
