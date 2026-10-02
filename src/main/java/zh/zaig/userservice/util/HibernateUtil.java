package zh.zaig.userservice.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zh.zaig.userservice.entity.User;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class HibernateUtil {

    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);

    private static SessionFactory sessionFactory;
    private static StandardServiceRegistry registry;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");
            //регистрация аннотированных классов
            configuration.addAnnotatedClass(User.class);

            applyDbProperties(configuration);

            registry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            SessionFactory factory = configuration.buildSessionFactory(registry);
            log.info("Hibernate SessionFactory создана");
            return factory;
        } catch (Exception e) {
            log.error("Не удалось создать SessionFactory", e);
            if (registry != null) {
                StandardServiceRegistryBuilder.destroy(registry);
            }
            throw new ExceptionInInitializerError("Ошибка инициализации Hibernate: " + e.getMessage());
        }
    }

    // Если есть db.properties, параметры подключения берутся из него
    private static void applyDbProperties(Configuration configuration) {
        Properties props = new Properties();
        try (InputStream is = HibernateUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                return;
            }
            props.load(is);
        } catch (IOException e) {
            log.warn("Не удалось прочитать db.properties", e);
            return;
        }

        setIfPresent(configuration, "hibernate.connection.url", props.getProperty("db.url"));
        setIfPresent(configuration, "hibernate.connection.username", props.getProperty("db.user"));
        setIfPresent(configuration, "hibernate.connection.password", props.getProperty("db.password"));
    }

    private static void setIfPresent(Configuration configuration, String key, String value) {
        if (value != null && !value.isBlank()) {
            configuration.setProperty(key, value);
        }
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
