package edu.whpu;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.config.props.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.mode.repository.standalone.StandalonePersistRepositoryConfiguration;
import org.apache.shardingsphere.readwritesplitting.config.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.config.rule.ReadwriteSplittingDataSourceGroupRuleConfiguration;
import org.apache.shardingsphere.single.config.SingleRuleConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws SQLException {
        // 运行模式
        ModeConfiguration modeConfiguration = new ModeConfiguration("Standalone", new StandalonePersistRepositoryConfiguration("JDBC", new Properties()));

        // 构建真实数据源，一主两从
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        // 主数据源
        HikariDataSource masterDataSource = new HikariDataSource();
        masterDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        masterDataSource.setJdbcUrl("jdbc:mysql://1.15.233.144:3307/db_test?serverTimezone=UTC&useSSL=false");
        masterDataSource.setUsername("root");
        masterDataSource.setPassword("123456");
        dataSourceMap.put("ds_master", masterDataSource);
        // 从数据源
        HikariDataSource slaveDataSource1 = new HikariDataSource();
        slaveDataSource1.setDriverClassName("com.mysql.cj.jdbc.Driver");
        slaveDataSource1.setJdbcUrl("jdbc:mysql://1.15.233.144:3308/db_test?serverTimezone=UTC&useSSL=false");
        slaveDataSource1.setUsername("root");
        slaveDataSource1.setPassword("123456");
        dataSourceMap.put("ds_slave1", slaveDataSource1);

        HikariDataSource slaveDataSource2 = new HikariDataSource();
        slaveDataSource2.setDriverClassName("com.mysql.cj.jdbc.Driver");
        slaveDataSource2.setJdbcUrl("jdbc:mysql://1.15.233.144:3309/db_test?serverTimezone=UTC&useSSL=false");
        slaveDataSource2.setUsername("root");
        slaveDataSource2.setPassword("123456");
        dataSourceMap.put("ds_slave2", slaveDataSource2);

        // 构建读写分离规则
        ReadwriteSplittingDataSourceGroupRuleConfiguration dataSourceConfig = new ReadwriteSplittingDataSourceGroupRuleConfiguration(
                "ds_readwrite", "ds_master", Arrays.asList("ds_slave1", "ds_slave2"), "demo_weight_lb");
        Properties algorithmProps = new Properties();
        algorithmProps.setProperty("ds_slave1", "1");
        algorithmProps.setProperty("ds_slave2", "2");
        Map<String, AlgorithmConfiguration> algorithmConfigMap = new HashMap<>(2);
        algorithmConfigMap.put("demo_weight_lb", new AlgorithmConfiguration("WEIGHT", algorithmProps));
        ReadwriteSplittingRuleConfiguration ruleConfig = new ReadwriteSplittingRuleConfiguration(Collections.singleton(dataSourceConfig), algorithmConfigMap);

        // 单表规则：5.4+ 需显式声明要加载的单表，否则元数据里不存在该表，会 TableNotFoundException
        SingleRuleConfiguration singleRuleConfig = new SingleRuleConfiguration();
        singleRuleConfig.getTables().add("*.*");
        singleRuleConfig.setDefaultDataSource("ds_master");

        // 构建属性配置
        Properties props = new Properties();
        props.setProperty(ConfigurationPropertyKey.SQL_SHOW.getKey(), "true");

        Collection<RuleConfiguration> rules = Arrays.asList(ruleConfig, singleRuleConfig);
        DataSource dataSource = ShardingSphereDataSourceFactory.createDataSource(modeConfiguration, dataSourceMap, rules, props);

        String sql = "SELECT * from student";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.setInt(1, 10);
//            ps.setInt(2, 1000);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    System.out.println(rs.getString("name"));
                }
            }
        }

    }

}
