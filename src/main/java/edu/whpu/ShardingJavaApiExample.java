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
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 与 {@code sharding-tables-only.yaml} 等价的 Java API 分库分表配置：节点 {@code ds${0..1}.t_order${0..1}}，
 * 库策略为 {@code user_id} + INLINE（库 {@code user_id % 2}），表策略为 {@code order_no} + INLINE（表 {@code Math.abs(order_no.hashCode()) % 2}）。
 */
public final class ShardingJavaApiExample {

    public static void main(String[] args) throws SQLException {
        ModeConfiguration modeConfiguration = new ModeConfiguration(
                "Standalone",
                new StandalonePersistRepositoryConfiguration("JDBC", new Properties()));

        Map<String, DataSource> dataSourceMap = new HashMap<>(2);
        HikariDataSource ds0 = new HikariDataSource();
        ds0.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds0.setJdbcUrl("jdbc:mysql://47.106.155.227:3306/db_order?serverTimezone=UTC&useSSL=false");
        ds0.setUsername("root");
        ds0.setPassword("wenbin123456");
        dataSourceMap.put("ds0", ds0);

        HikariDataSource ds1 = new HikariDataSource();
        ds1.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds1.setJdbcUrl("jdbc:mysql://1.15.233.144:3307/db_order?serverTimezone=UTC&useSSL=false");
        ds1.setUsername("root");
        ds1.setPassword("123456");
        dataSourceMap.put("ds1", ds1);

        ShardingTableRuleConfiguration orderTable = new ShardingTableRuleConfiguration(
                "t_order",
                "ds${0..1}.t_order${0..1}");
        orderTable.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("user_id", "alg_inline_userid"));
        orderTable.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("order_no", "alg_hash_mod_order_no"));

        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        shardingRuleConfiguration.getTables().add(orderTable);

        Properties databaseInlineProps = new Properties();
        databaseInlineProps.setProperty("algorithm-expression", "ds$->{user_id % 2}");
        shardingRuleConfiguration.getShardingAlgorithms().put("alg_inline_userid", new AlgorithmConfiguration("INLINE", databaseInlineProps));

        Properties tableInlineProps = new Properties();
        tableInlineProps.setProperty("algorithm-expression", "t_order$->{Math.abs(order_no.hashCode()) % 2}");
        shardingRuleConfiguration.getShardingAlgorithms().put("alg_hash_mod_order_no", new AlgorithmConfiguration("INLINE", tableInlineProps));

        Properties props = new Properties();
        props.setProperty(ConfigurationPropertyKey.SQL_SHOW.getKey(), "true");

        Collection<RuleConfiguration> rules = Collections.singletonList(shardingRuleConfiguration);
        DataSource dataSource = ShardingSphereDataSourceFactory.createDataSource(
                "sharding_db_table_demo",
                modeConfiguration,
                dataSourceMap,
                rules,
                props);

        String sql = "INSERT INTO t_order (id, order_no, user_id, amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            java.util.Random random = new java.util.Random();
            for (int i = 1; i <= 10; i++) {
                long id = Math.abs(random.nextLong()) + 1;
                String orderNo = "ORD" + String.format("%03d", i);
                long userId = random.nextInt(100) + 1;
                java.math.BigDecimal amount = java.math.BigDecimal.valueOf(100 + random.nextDouble() * 900).setScale(2, java.math.RoundingMode.HALF_UP);
                ps.setLong(1, id);
                ps.setString(2, orderNo);
                ps.setLong(3, userId);
                ps.setBigDecimal(4, amount);
                ps.executeUpdate();
                System.out.println("Inserted order: id=" + id + ", order_no=" + orderNo + ", user_id=" + userId + ", amount=" + amount);
            }
        }
    }

    private ShardingJavaApiExample() {
    }
}
