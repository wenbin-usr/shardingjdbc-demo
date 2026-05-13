-- 分库分表：与 sharding-tables-only.yaml / ShardingJavaApiExample 配套
-- 请在「每个」分片库上执行本脚本（本例为 3307、3308 上的 db_test 各执行一次）
-- 路由：id=0,1 -> ds_0；id=2,3 -> ds_1 的库下标与 id.intdiv(2)%2 一致；表后缀为 id%2

CREATE TABLE IF NOT EXISTS student_0 (
    id BIGINT NOT NULL PRIMARY KEY,
    stuno VARCHAR(64),
    age INT,
    name VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS student_1 (
    id BIGINT NOT NULL PRIMARY KEY,
    stuno VARCHAR(64),
    age INT,
    name VARCHAR(128)
);
