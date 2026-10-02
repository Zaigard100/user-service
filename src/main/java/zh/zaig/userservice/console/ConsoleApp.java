package zh.zaig.userservice.console;

import zh.zaig.userservice.entity.User;
import zh.zaig.userservice.exception.DataAccessException;
import zh.zaig.userservice.exception.UserNotFoundException;
import zh.zaig.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class ConsoleApp {

    private static final Logger log = LoggerFactory.getLogger(ConsoleApp.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService;

    public ConsoleApp(UserService userService) {
        this.userService = userService;
    }

    public void run() {
        printWelcome();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> createUser();
                    case "2" -> findUserById();
                    case "3" -> listAllUsers();
                    case "4" -> updateUser();
                    case "5" -> deleteUser();
                    case "0" -> {
                        running = false;
                        System.out.println("До свидания!");
                    }
                    default -> System.out.println("Неизвестный пункт меню, попробуйте снова.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка ввода: " + e.getMessage());
                log.warn("Ошибка валидации: {}", e.getMessage());
            } catch (UserNotFoundException e) {
                System.out.println(e.getMessage());
                log.warn("Сущность не найдена: {}", e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("Ошибка при обращении к базе данных: " + e.getMessage());
                System.out.println("Подробности записаны в лог-файл (logs/user-service.log).");
            } catch (Exception e) {
                System.out.println("Непредвиденная ошибка: " + e.getMessage());
                log.error("Непредвиденная ошибка в консольном приложении", e);
            }
            System.out.println();
        }
    }

    private void printWelcome() {
        System.out.println("=== User Service ===");
    }

    private void printMenu() {
        System.out.println("Выберите действие:");
        System.out.println(" 1 - Создать пользователя");
        System.out.println(" 2 - Найти пользователя по id");
        System.out.println(" 3 - Показать всех пользователей");
        System.out.println(" 4 - Обновить пользователя");
        System.out.println(" 5 - Удалить пользователя");
        System.out.println(" 0 - Выход");
        System.out.print("> ");
    }

    private void createUser() {
        System.out.print("Имя: ");
        String name = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        Integer age = readOptionalInt("Возраст (можно оставить пустым): ");

        User created = userService.createUser(name, email, age);
        System.out.println("Пользователь создан:");
        printUser(created);
    }

    private void findUserById() {
        Long id = readRequiredLong("Введите id пользователя: ");
        User user = userService.getUserById(id);
        printUser(user);
    }

    private void listAllUsers() {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("В базе данных пока нет пользователей.");
            return;
        }
        System.out.printf("%-5s %-20s %-30s %-5s %-20s%n", "ID", "Имя", "Email", "Возраст", "Создан");
        System.out.println("-".repeat(90));
        for (User u : users) {
            System.out.printf("%-5d %-20s %-30s %-5s %-20s%n",
                    u.getId(),
                    u.getName(),
                    u.getEmail(),
                    u.getAge() != null ? u.getAge() : "-",
                    u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FORMAT) : "-");
        }
    }

    private void updateUser() {
        Long id = readRequiredLong("Введите id пользователя, которого нужно обновить: ");

        System.out.print("Новое имя (Enter - не менять): ");
        String name = scanner.nextLine();

        System.out.print("Новый email (Enter - не менять): ");
        String email = scanner.nextLine();

        Integer age = readOptionalInt("Новый возраст (Enter - не менять): ");

        User updated = userService.updateUser(
                id,
                name.isBlank() ? null : name,
                email.isBlank() ? null : email,
                age);
        System.out.println("Пользователь обновлён:");
        printUser(updated);
    }

    private void deleteUser() {
        Long id = readRequiredLong("Введите id пользователя, которого нужно удалить: ");
        userService.deleteUser(id);
        System.out.println("Пользователь с id=" + id + " удалён.");
    }

    private void printUser(User u) {
        System.out.println("---------------------------------------------");
        System.out.println("ID:         " + u.getId());
        System.out.println("Имя:        " + u.getName());
        System.out.println("Email:      " + u.getEmail());
        System.out.println("Возраст:    " + (u.getAge() != null ? u.getAge() : "-"));
        System.out.println("Создан:     " + (u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FORMAT) : "-"));
        System.out.println("---------------------------------------------");
    }

    private Long readRequiredLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("Некорректный формат числа, попробуйте снова.");
            }
        }
    }

    private Integer readOptionalInt(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Некорректный формат числа, значение проигнорировано.");
            return null;
        }
    }
}
