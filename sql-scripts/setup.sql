-- Tạo bảng users
CREATE TABLE users (
            id INT PRIMARY KEY AUTO_INCREMENT,
            username VARCHAR(50) NOT NULL UNIQUE,
            email VARCHAR(100) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            full_name VARCHAR(100),
            avatar_url VARCHAR(255) DEFAULT '/images/avatar/default-avatar.png',
            points INT DEFAULT 0 NOT NULL,
            enabled tinyint NOT NULL DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tạo bảng authorities để sử dụng cho spring security
CREATE TABLE authorities (
            username VARCHAR(50) NOT NULL,
            authority VARCHAR(50) NOT NULL,
            UNIQUE KEY (username, authority),
            FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

-- Trigger để tự tạo ROLE_USER cho user mới
DELIMITER //

CREATE TRIGGER after_user_insert
    AFTER INSERT ON users
    FOR EACH ROW
BEGIN
    INSERT INTO authorities (username, authority)
    VALUES (NEW.username, 'ROLE_USER');
END//

DELIMITER ;

-- Tạo bảng stories
CREATE TABLE stories (
            id INT PRIMARY KEY AUTO_INCREMENT,
            title VARCHAR(255) NOT NULL,
            author VARCHAR(100),
            description TEXT,
            cover_image TEXT,
            views INT DEFAULT 0 NOT NULL,
            status ENUM('ongoing', 'completed', 'dropped') DEFAULT 'ongoing',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tạo bảng genres
CREATE TABLE genres (
            id INT PRIMARY KEY AUTO_INCREMENT,
            name VARCHAR(50) NOT NULL UNIQUE,
            description TEXT
);

-- Tạo bảng story_genres
CREATE TABLE story_genres (
            story_id INT,
            genre_id INT,
            PRIMARY KEY (story_id, genre_id),
            FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
            FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

-- Tạo bảng chapters
CREATE TABLE chapters (
            id INT PRIMARY KEY AUTO_INCREMENT,
            story_id INT NOT NULL,
            chapter_number INT NOT NULL,
            title VARCHAR(255),
            content MEDIUMTEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);


-- Tạo bảng comments
CREATE TABLE comments (
            id INT PRIMARY KEY AUTO_INCREMENT,
            user_id INT NOT NULL,
            story_id INT,
            chapter_id INT,
            content TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
            FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);


-- Tạo bảng bookmarks
CREATE TABLE bookmarks (
            user_id INT,
            story_id INT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (user_id, story_id),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

-- Tạo bảng reading_histories
CREATE TABLE reading_histories (
            id INT PRIMARY KEY AUTO_INCREMENT,
            user_id INT NOT NULL,
            story_id INT NOT NULL,
            chapter_id INT NOT NULL,
            last_read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
            FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);

-- Tạo bảng ratings
CREATE TABLE ratings (
            id INT PRIMARY KEY AUTO_INCREMENT,
            user_id INT NOT NULL,
            story_id INT NOT NULL,
            score INT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE (user_id, story_id),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

-- Tạo bảng voices
CREATE TABLE voices (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL UNIQUE,
                        gender VARCHAR(10)
);