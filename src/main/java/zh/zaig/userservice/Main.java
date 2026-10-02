package zh.zaig.userservice;

import zh.zaig.userservice.console.ConsoleApp;
import zh.zaig.userservice.service.UserService;
import zh.zaig.userservice.util.HibernateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Запуск приложения");
        try {
            /*
            Здесь создается сервис, который внутри себя создает DAO пользователя.
            DAO пользователя запрашивает SessionFactory, который если не существует,
            создаётся на основе конфигурации hibernate.cfg.xml
             */
            UserService userService = new UserService();
            /*
            Создает консольное приложение через которое происходит управление сервисом
             */
            ConsoleApp app = new ConsoleApp(userService);
            app.run();
        } catch (ExceptionInInitializerError e) {
            System.err.println("Не удалось подключиться к базе данных.");
            System.err.println(e.getMessage());
            log.error("Ошибка инициализации", e);
        } finally {
            HibernateUtil.shutdown();
            log.info("Приложение завершило работу");
        }
    }
}
