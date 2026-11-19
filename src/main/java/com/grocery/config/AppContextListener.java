package com.grocery.config;

import com.grocery.dao.CategoryDao;
import com.grocery.dao.ProductDao;
import com.grocery.dao.SaleDao;
import com.grocery.dao.jdbc.JdbcCategoryDao;
import com.grocery.dao.jdbc.JdbcProductDao;
import com.grocery.dao.jdbc.JdbcSaleDao;
import com.grocery.dao.jdbc.JdbcUserDao;
import com.grocery.service.AuthService;
import com.grocery.service.DashboardService;
import com.grocery.service.LoginAttemptLimiter;
import com.grocery.service.ProductService;
import com.grocery.service.SaleService;
import com.grocery.util.PasswordHasher;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.time.Clock;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Wires the app together at startup (a hand-rolled composition root, no DI
 * framework) and closes the connection pool at shutdown.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    private HikariDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        AppConfig config = AppConfig.fromEnvironment();
        LOG.info("Starting GroceryIQ with " + config);

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.dbUrl());
        hikari.setUsername(config.dbUser());
        hikari.setPassword(config.dbPassword());
        hikari.setMaximumPoolSize(config.dbPoolSize());
        hikari.setPoolName("grocery-pool");
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        // Don't fail startup if MySQL is still booting (e.g. in docker compose).
        hikari.setInitializationFailTimeout(-1);
        dataSource = new HikariDataSource(hikari);

        Clock clock = Clock.systemDefaultZone();
        ProductDao productDao = new JdbcProductDao(dataSource);
        CategoryDao categoryDao = new JdbcCategoryDao(dataSource);
        SaleDao saleDao = new JdbcSaleDao(dataSource);

        AppServices services = new AppServices(
                new AuthService(new JdbcUserDao(dataSource), new PasswordHasher(),
                        new LoginAttemptLimiter(5, Duration.ofMinutes(5), clock)),
                new ProductService(productDao, categoryDao, clock),
                new SaleService(saleDao, productDao, clock),
                new DashboardService(productDao, categoryDao, saleDao, clock));

        event.getServletContext().setAttribute(AppServices.ATTRIBUTE, services);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (dataSource != null) dataSource.close();
    }
}
