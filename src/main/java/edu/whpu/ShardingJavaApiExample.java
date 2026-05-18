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
 * 库策略为 {@code user_id} + INLINE（库 {@code user_id % 2}），表策略为 {@code order_no} + INLINE（{@code hashCode % 2}）。
 * <p>说明：{@code HASH_MOD}/{@code MOD} 属于自动分片算法，只能配在 {@code autoTables} 上；本示例使用 {@code actualDataNodes} 手动节点，表分片须用 INLINE。
 */
public final class ShardingJavaApiExample {

    public static void main(String[] args) throws SQLException {
        ModeConfiguration modeConfiguration = new ModeConfiguration(
                "Standalone",
                new StandalonePersistRepositoryConfiguration("JDBC", new Properties()));

        // 两个物理库，逻辑上通过分片规则映射为 ds0、ds1
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

        // 手动分表：显式声明 4 个数据节点（2 库 × 2 表），须配合 INLINE 等「标准」分片算法
        // 不能使用 HASH_MOD/MOD：自 5.4.1 起它们属于自动分片算法，只能用于 autoTables
        ShardingTableRuleConfiguration orderTable = new ShardingTableRuleConfiguration(
                "t_order",
                "ds${0..1}.t_order${0..1}");
        // 分库：按 user_id 取余路由到 ds0 / ds1
        orderTable.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("user_id", "alg_inline_userid"));
        // 分表：按 order_no 哈希取模路由到 t_order0 / t_order1
        orderTable.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("order_no", "alg_hash_mod_order_no"));

        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        shardingRuleConfiguration.getTables().add(orderTable);

        Properties databaseInlineProps = new Properties();
        databaseInlineProps.setProperty("algorithm-expression", "ds$->{user_id % 2}");
        shardingRuleConfiguration.getShardingAlgorithms().put("alg_inline_userid", new AlgorithmConfiguration("INLINE", databaseInlineProps));

        // 表分片用 INLINE 实现 hashCode % 2，效果与 HASH_MOD(sharding-count=2) 对 String 相同
        // 若要用 HASH_MOD，需改为 ShardingAutoTableRuleConfiguration + autoTables，且物理表名规则可能变化
        Properties tableInlineProps = new Properties();
        tableInlineProps.setProperty("algorithm-expression", "t_order$->{Math.abs(order_no.hashCode()) % 2}");
        shardingRuleConfiguration.getShardingAlgorithms().put("alg_hash_mod_order_no", new AlgorithmConfiguration("INLINE", tableInlineProps));

        Properties props = new Properties();
        // 打印逻辑 SQL / 实际 SQL（输出到 ShardingSphere-SQL 日志，需配置 logback）
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
