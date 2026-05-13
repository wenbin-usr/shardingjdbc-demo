package edu.whpu;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.config.props.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.mode.repository.standalone.StandalonePersistRepositoryConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 与 {@code sharding-tables-only.yaml} 等价的 Java API 分库分表配置：节点 {@code ds_${0..1}.student_${0..1}}，
 * 库/表策略均为 {@code id} + INLINE（库 {@code id.intdiv(2) % 2}，表 {@code id % 2}）。
 */
public final class ShardingJavaApiExample {

    public static void main(String[] args) throws SQLException {
        ModeConfiguration modeConfiguration = new ModeConfiguration(
                "Standalone",
                new StandalonePersistRepositoryConfiguration("JDBC", new Properties()));

        Map<String, DataSource> dataSourceMap = new HashMap<>(2);
        HikariDataSource ds0 = new HikariDataSource();
        ds0.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds0.setJdbcUrl("jdbc:mysql://1.15.233.144:3307/db_test?serverTimezone=UTC&useSSL=false");
        ds0.setUsername("root");
        ds0.setPassword("123456");
        dataSourceMap.put("ds_0", ds0);

        HikariDataSource ds1 = new HikariDataSource();
        ds1.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds1.setJdbcUrl("jdbc:mysql://1.15.233.144:3308/db_test?serverTimezone=UTC&useSSL=false");
        ds1.setUsername("root");
        ds1.setPassword("123456");
        dataSourceMap.put("ds_1", ds1);

        ShardingTableRuleConfiguration studentTable = new ShardingTableRuleConfiguration(
                "student",
                "ds_${0..1}.student_${0..1}");
        studentTable.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("id", "student_database_inline"));
        studentTable.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("id", "student_table_inline"));

        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        shardingRuleConfiguration.getTables().add(studentTable);

        Properties databaseInlineProps = new Properties();
        databaseInlineProps.setProperty("algorithm-expression", "ds_${id.intdiv(2) % 2}");
        shardingRuleConfiguration.getShardingAlgorithms().put(
                "student_database_inline",
                new AlgorithmConfiguration("INLINE", databaseInlineProps));

        Properties tableInlineProps = new Properties();
        tableInlineProps.setProperty("algorithm-expression", "student_${id % 2}");
        shardingRuleConfiguration.getShardingAlgorithms().put(
                "student_table_inline",
                new AlgorithmConfiguration("INLINE", tableInlineProps));

        Properties props = new Properties();
        props.setProperty(ConfigurationPropertyKey.SQL_SHOW.getKey(), "true");

        Collection<RuleConfiguration> rules = Collections.singletonList(shardingRuleConfiguration);
        DataSource dataSource = ShardingSphereDataSourceFactory.createDataSource(
                "sharding_db_table_demo",
                modeConfiguration,
                dataSourceMap,
                rules,
                props);

        String sql = "SELECT id, stuno, age, name FROM student WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // id=103 -> ds_1.student_1
            ps.setLong(1, 103L);
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

    private ShardingJavaApiExample() {
    }
}
