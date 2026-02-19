package by.example.MusicBot.repository;

import by.example.MusicBot.domain.GuildSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация GuildSessionRepository.
 * 
 * <p>Хранит сессии в оперативной памяти используя {@link ConcurrentHashMap}.
 * Это простое и быстрое решение для бота, который работает на одном сервере.
 * 
 * <h3>Преимущества:</h3>
 * <ul>
 *     <li><b>Быстрый доступ:</b> O(1) для получения/создания сессии</li>
 *     <li><b>Потокобезопасность:</b> ConcurrentHashMap позволяет работать из нескольких потоков</li>
 *     <li><b>Простота:</b> не требует настройки базы данных</li>
 * </ul>
 * 
 * <h3>Недостатки:</h3>
 * <ul>
 *     <li><b>Потеря данных:</b> все сессии теряются при перезапуске бота</li>
 *     <li><b>Ограниченная память:</b> количество сессий ограничено RAM</li>
 * </ul>
 * 
 * <h3>Потокобезопасность:</h3>
 * <p>ConcurrentHashMap гарантирует, что несколько потоков могут безопасно:
 * <ul>
 *     <li>Получать сессии одновременно</li>
 *     <li>Создавать новые сессии</li>
 *     <li>Удалять сессии</li>
 * </ul>
 * 
 * <p>Пример использования:
 * <pre>{@code
 * GuildSessionRepository repo = new InMemoryGuildSessionRepository();
 * GuildSession session = repo.getOrCreate(123456789L);
 * }</pre>
 * 
 * @see GuildSessionRepository
 * @see ConcurrentHashMap
 */
public class InMemoryGuildSessionRepository implements GuildSessionRepository {
    /** Логгер для записи событий в лог */
    private static final Logger logger = LoggerFactory.getLogger(InMemoryGuildSessionRepository.class);

    /** 
     * Хранилище сессий: ID гильдии → сессия.
     * 
     * <p>ConcurrentHashMap выбран потому что:
     * <ul>
     *     <li>Потоки JDA могут обращаться к сессиям одновременно</li>
     *     <li>Поток Lavaplayer тоже может модифицировать сессии</li>
     *     <li>Нужна гарантия отсутствия race conditions</li>
     * </ul>
     */
    private final Map<Long, GuildSession> sessions;

    /**
     * Создаёт новый репозиторий с пустым хранилищем.
     * 
     * <p>После создания репозиторий не содержит сессий.
     * Сессии создаются при первом обращении через {@link #getOrCreate(long)}.
     */
    public InMemoryGuildSessionRepository() {
        this.sessions = new ConcurrentHashMap<>();
        logger.info("InMemoryGuildSessionRepository инициализирован");
    }

    /**
     * Получает сессию или создаёт новую.
     * 
     * <p>Использует {@link Map#computeIfAbsent(Object, Function)} для
     * атомарного создания сессии. Это гарантирует, что даже при
     * одновременном вызове из нескольких потоков будет создана
     * только одна сессия.
     * 
     * @param guildId ID гильдии
     * @return существующая или новая сессия
     */
    @Override
    public GuildSession getOrCreate(long guildId) {
        return sessions.computeIfAbsent(guildId, id -> {
            GuildSession session = new GuildSession(id);
            logger.debug("Создана новая сессия для гильдии: {}", guildId);
            return session;
        });
    }

    /**
     * Получает сессию по ID гильдии.
     * 
     * <p>Возвращает Optional, который может содержать сессию
     * или быть пустым если сессии нет.
     * 
     * @param guildId ID гильдии
     * @return Optional с сессией или пустой Optional
     */
    @Override
    public Optional<GuildSession> get(long guildId) {
        return Optional.ofNullable(sessions.get(guildId));
    }

    /**
     * Удаляет сессию гильдии.
     * 
     * <p>Если сессии не существует, метод ничего не делает.
     * 
     * @param guildId ID гильдии для удаления
     */
    @Override
    public void remove(long guildId) {
        GuildSession removed = sessions.remove(guildId);
        if (removed != null) {
            logger.debug("Удалена сессия для гильдии: {}", guildId);
        }
    }

    /**
     * Проверяет наличие сессии.
     * 
     * @param guildId ID гильдии
     * @return true если сессия существует
     */
    @Override
    public boolean exists(long guildId) {
        return sessions.containsKey(guildId);
    }

    /**
     * Очищает все сессии.
     * 
     * <p>После вызова этого метода {@link #getActiveSessionCount()}
     * вернёт 0, и все сессии будут потеряны.
     */
    @Override
    public void clearAll() {
        int count = sessions.size();
        sessions.clear();
        logger.info("Очищено {} сессий", count);
    }

    /**
     * Подсчитывает активные сессии.
     * 
     * <p>Использует Stream API для фильтрации:
     * <ol>
     *     <li>Берёт все сессии из values()</li>
     *     <li>Фильтрует только активные через GuildSession::isActive</li>
     *     <li>Подсчитывает количество через count()</li>
     * </ol>
     * 
     * @return количество активных сессий
     */
    @Override
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(GuildSession::isActive)
                .count();
    }
}
