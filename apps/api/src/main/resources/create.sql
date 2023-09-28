DROP TABLE if exists agenda;

CREATE TABLE agenda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE,
    type VARCHAR(10),
    begin_time TIME,
    end_time TIME,
    title VARCHAR(255),
    user_id VARCHAR(255)
);
