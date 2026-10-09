-- Inicialización de base de datos para Tappazo
CREATE DATABASE IF NOT EXISTS `tappazo_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON `tappazo_db`.* TO 'tappazo'@'%';
FLUSH PRIVILEGES;
