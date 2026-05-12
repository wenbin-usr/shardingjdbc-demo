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
 * 基于 classpath 下 YAML 的 ShardingSphere-JDBC 读写分离示例。
 * 配置见 {@code readwrite-sharding.yaml}，库地址与 {@link App} 中一致。
 */
public final class ReadwriteYamlExample {

    private static final String YAML_RESOURCE = "readwrite-sharding.yaml";

    public static void main(String[] args) throws SQLException, IOException {
        byte[] yamlBytes = readClasspathResource(YAML_RESOURCE);
        DataSource dataSource = YamlShardingSphereDataSourceFactory.createDataSource(yamlBytes);

        String sql = "SELECT * FROM student";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.println(rs.getString("name"));
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

    private ReadwriteYamlExample() {
    }
}
