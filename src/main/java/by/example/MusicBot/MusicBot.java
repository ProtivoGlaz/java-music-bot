package by.example.MusicBot;

import by.example.MusicBot.config.DependencyInjector;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа приложения MusicBot.
 * Инициализирует JDA и контейнер зависимостей.
 */
public class MusicBot {
    private static final Logger logger = LoggerFactory.getLogger(MusicBot.class);

    private JDA jda;
    private DependencyInjector injector;

    public static void main(String[] args) {
        new MusicBot().start();
    }

    /**
     * Запустить бота.
     */
    public void start() {
        logger.info("=== Запуск MusicBot ===");

        String token = getBotToken();
        if (token == null || token.isEmpty()) {
            logger.error("Токен Discord не найден!");
            logger.error("Установите переменную окружения DISCORD_TOKEN");
            System.exit(1);
        }

        try {
            // Создаём JDA
            jda = createJda(token);
            
            // Инициализируем контейнер зависимостей
            injector = new DependencyInjector(jda);
            injector.initialize();
            
            // Регистрируем listener
            jda.addEventListener(injector.getCommandListener());
            
            // Ждём готовности
            jda.awaitReady();
            
            logger.info("=== Бот успешно запущен ===");
            logger.info("Бот: {}", jda.getSelfUser().getName());
            logger.info("Серверов: {}", jda.getGuildCache().size());
            
            // Регистрируем shutdown hook
            registerShutdownHook();
            
        } catch (Exception e) {
            logger.error("Ошибка при запуске бота", e);
            shutdown();
            System.exit(1);
        }
    }

    /**
     * Получить токен бота.
     */
    private String getBotToken() {
        // Пробуем разные способы получения токена
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isEmpty()) {
            token = System.getProperty("discord.token");
        }
        return token;
    }

    /**
     * Создать экземпляр JDA.
     */
    private JDA createJda(String token) throws InterruptedException {
        logger.info("Создание JDA instance...");
        
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_VOICE_STATES
                )
                .build();
        
        return jda;
    }

    /**
     * Зарегистрировать обработчик завершения.
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Получен сигнал завершения...");
            shutdown();
        }, "shutdown-hook"));
    }

    /**
     * Корректное завершение работы.
     */
    private void shutdown() {
        logger.info("Завершение работы бота...");
        
        if (injector != null) {
            injector.shutdown();
        }
        
        if (jda != null) {
            jda.shutdown();
            try {
                jda.awaitShutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Бот остановлен");
    }
}
