-- Optional: run this manually if you prefer to create the schema/table yourself
-- instead of relying on spring.jpa.hibernate.ddl-auto=update

CREATE DATABASE IF NOT EXISTS studentdb;

USE studentdb;

CREATE TABLE IF NOT EXISTS student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL
);

-- Sample data (optional)
INSERT INTO student (name, department) VALUES ('Alice Johnson', 'Computer Science');
INSERT INTO student (name, department) VALUES ('Bob Smith', 'Mechanical Engineering');
