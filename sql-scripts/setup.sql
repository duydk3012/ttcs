CREATE TABLE users (
                       id INT PRIMARY KEY AUTO_INCREMENT,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100),
                       avatar_url VARCHAR(255) DEFAULT '/images/avatar/user.png',
                       enabled tinyint NOT NULL DEFAULT 1,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE authorities (
                             username VARCHAR(50) NOT NULL,
                             authority VARCHAR(50) NOT NULL,
                             UNIQUE KEY (username, authority),
                             FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

DELIMITER //

CREATE TRIGGER after_user_insert
    AFTER INSERT ON users
    FOR EACH ROW
BEGIN
    INSERT INTO authorities (username, authority)
    VALUES (NEW.username, 'ROLE_USER');
END//

DELIMITER ;