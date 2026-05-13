package edu.whpu;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 基于 classpath YAML 的分库分表示例：逻辑表 {@code student}，节点 {@code ds_${0..1}.student_${0..1}}，
 * 库/表均按 {@code id} 路由（INLINE：库 {@code id.intdiv(2) % 2}，表 {@code id % 2}）。配置见 {@code sharding-tables-only.yaml}。
 */
public final class ShardingYamlExample {

    private static final String YAML_RESOURCE = "sharding-tables-only.yaml";

    public static void main(String[] args) throws SQLException, IOException {
        byte[] yamlBytes = readClasspathResource(YAML_RESOURCE);
        DataSource dataSource = YamlShardingSphereDataSourceFactory.createDataSource(yamlBytes);

        // 带分片键，路由到单表；可在控制台观察 sql-show 输出的实际 SQL
        String sql = "SELECT id, stuno, age, name FROM student WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // id=102 -> ds_1.student_0（可在日志中核对 sql-show）
            ps.setLong(1, 102L);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println(new Student(
                            rs.getLong("id"),
                            rs.getString("stuno"),
                            rs.getInt("age"),
                            rs.getString("name")));
                }
            }
        }
    }

    private static byte[] readClasspathResource(String name) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl != null ? cl.getResourceAsStream(name) : null) {
            if (in == null) {
                throw new IOException("Classpath 上找不到资源: " + name);
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
            }
            return buf.toByteArray();
        }
    }

    private ShardingYamlExample() {
    }
}
