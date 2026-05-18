CREATE DATABASE db_order;
USE db_order;

CREATE TABLE t_order0 (
                          id BIGINT,
                          order_no VARCHAR(30),
                          user_id BIGINT,
                          amount DECIMAL(10,2),
                          PRIMARY KEY(id)
);

CREATE TABLE t_order1 (
                          id BIGINT,
                          order_no VARCHAR(30),
                          user_id BIGINT,
                          amount DECIMAL(10,2),
                          PRIMARY KEY(id)
);