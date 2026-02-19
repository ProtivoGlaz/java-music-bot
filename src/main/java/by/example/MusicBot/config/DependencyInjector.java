package by.example.MusicBot.config;

import by.example.MusicBot.infrastructure.AudioPlayerRegistry;
import by.example.MusicBot.infrastructure.LavaplayerAudioPlayerRegistry;
import by.example.MusicBot.listener.CommandListener;
import by.example.MusicBot.repository.GuildSessionRepository;
import by.example.MusicBot.repository.InMemoryGuildSessionRepository;
import by.example.MusicBot.service.MusicService;
import by.example.MusicBot.service.MusicServiceImpl;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контейнер зависимостей приложения.
 * Собирает все слои архитектуры в единую систему.
 * 
 * Архитектурные слои:
 * ┌─────────────────────────────────┐
 * │    Presentation Layer           │
 * │    (CommandListener)            │
 * ├─────────────────────────────────┤
 * │    Service Layer                │
 * │    (MusicService)               │
 * ├─────────────────────────────────┤
 * │    Repository Layer             │
 * │    (GuildSessionRepository)     │
 * ├─────────────────────────────────┤
 * │    Infrastructure Layer         │
 * │    (AudioPlayerRegistry)        │
 * └─────────────────────────────────┘
 */
public class DependencyInjector {
    private static final Logger logger = LoggerFactory.getLogger(DependencyInjector.class);

    private final JDA jda;
    
    // Infrastructure layer
    private AudioPlayerRegistry audioPlayerRegistry;
    
    // Repository layer
    private GuildSessionRepository sessionRepository;
    
    // Service layer
    private MusicService musicService;
    
    // Presentation layer
    private CommandListener commandListener;

    public DependencyInjector(JDA jda) {
        this.jda = jda;
    }

    /**
     * Инициализировать все зависимости.
     */
    public void initialize() {
        logger.info("=== Инициализация зависимостей ===");
        
        // 1. Infrastructure layer (зависит от JDA)
        createInfrastructureLayer();
        
        // 2. Repository layer
        createRepositoryLayer();
        
        // 3. Service layer (зависит от Repository и Infrastructure)
        createServiceLayer();
        
        // 4. Presentation layer (зависит от Service)
        createPresentationLayer();
        
        logger.info("=== Все зависимости инициализированы ===");
    }

    private void createInfrastructureLayer() {
        logger.info("Создание Infrastructure layer...");
        this.audioPlayerRegistry = new LavaplayerAudioPlayerRegistry(jda);
    }

    private void createRepositoryLayer() {
        logger.info("Создание Repository layer...");
        this.sessionRepository = new InMemoryGuildSessionRepository();
    }

    private void createServiceLayer() {
        logger.info("Создание Service layer...");
        this.musicService = new MusicServiceImpl(sessionRepository, audioPlayerRegistry);
    }

    private void createPresentationLayer() {
        logger.info("Создание Presentation layer...");
        this.commandListener = new CommandListener(musicService);
    }

    // Геттеры для использования вне контейнера

    public JDA getJDA() {
        return jda;
    }

    public AudioPlayerRegistry getAudioPlayerRegistry() {
        return audioPlayerRegistry;
    }

    public GuildSessionRepository getSessionRepository() {
        return sessionRepository;
    }

    public MusicService getMusicService() {
        return musicService;
    }

    public CommandListener getCommandListener() {
        return commandListener;
    }

    /**
     * Освободить ресурсы.
     */
    public void shutdown() {
        logger.info("Завершение работы...");
        
        if (sessionRepository != null) {
            sessionRepository.clearAll();
        }
        
        logger.info("Ресурсы освобождены");
    }
}
