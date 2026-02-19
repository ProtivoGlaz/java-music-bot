package by.example.MusicBot.service;

import by.example.MusicBot.domain.GuildSession;
import by.example.MusicBot.domain.Track;
import by.example.MusicBot.infrastructure.AudioPlayerRegistry;
import by.example.MusicBot.repository.GuildSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Реализация MusicService — основной компонент бизнес-логики.
 * 
 * <p>Этот класс координирует работу между:
 * <ul>
 *     <li><b>Repository layer</b> — получение/сохранение сессий</li>
 *     <li><b>Infrastructure layer</b> — воспроизведение музыки</li>
 * </ul>
 * 
 * <h3>Архитектурное место:</h3>
 * <pre>
 * ┌─────────────────────┐
 * │  CommandListener    │ ← Presentation layer
 * └─────────┬───────────┘
 *           │ вызывает
 *           ▼
 * ┌─────────────────────┐
 * │ MusicServiceImpl    │ ← Service layer (этот класс)
 * └─────────┬───────────┘
 *           │ использует
 *           ▼
 * ┌─────────────────────┐
 * │  AudioPlayerRegistry│ ← Infrastructure layer
 * └─────────────────────┘
 * </pre>
 * 
 * <h3>Пример потока выполнения !play:</h3>
 * <ol>
 *     <li>CommandListener вызывает loadAndQueue()</li>
 *     <li>Сервис получает сессию из репозитория</li>
 *     <li>Загружает трек через AudioPlayerRegistry</li>
 *     <li>Добавляет трек в очередь сессии</li>
 *     <li>Передаёт трек в плеер для воспроизведения</li>
 * </ol>
 */
public class MusicServiceImpl implements MusicService {
    /** Логгер для записи событий */
    private static final Logger logger = LoggerFactory.getLogger(MusicServiceImpl.class);

    /** Репозиторий для доступа к сессиям гильдий */
    private final GuildSessionRepository sessionRepository;
    
    /** Регистри плееров для управления воспроизведением */
    private final AudioPlayerRegistry playerRegistry;

    /**
     * Конструктор с внедрением зависимостей.
     * 
     * @param sessionRepository репозиторий сессий
     * @param playerRegistry регистр аудиоплееров
     */
    public MusicServiceImpl(GuildSessionRepository sessionRepository, 
                           AudioPlayerRegistry playerRegistry) {
        this.sessionRepository = sessionRepository;
        this.playerRegistry = playerRegistry;
        logger.info("MusicServiceImpl инициализирован");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Поток выполнения:
     * <ol>
     *     <li>Загружает трек через playerRegistry.loadTrack()</li>
     *     <li>Получает или создаёт сессию для гильдии</li>
     *     <li>Добавляет треки в очередь сессии</li>
     *     <li>Передаёт треки в плеер через queueTrack()</li>
     * </ol>
     */
    @Override
    public CompletableFuture<TrackLoadResult> loadAndQueue(long guildId, String query) {
        return playerRegistry.loadTrack(query)
                .thenApply(result -> {
                    if (result.getStatus() == TrackLoadResult.LoadStatus.TRACK_LOADED ||
                        result.getStatus() == TrackLoadResult.LoadStatus.PLAYLIST_LOADED) {
                        
                        GuildSession session = sessionRepository.getOrCreate(guildId);
                        boolean wasEmpty = session.getQueue().isEmpty() && 
                                          session.getQueue().getCurrentTrack().isEmpty();
                        
                        // Добавляем треки в domain очередь и infrastructure плеер
                        result.getTracks().forEach(track -> {
                            session.getQueue().add(track);
                            playerRegistry.queueTrack(guildId, track);
                        });
                        
                        // Если очередь была пустой, помечаем сессию как активную
                        if (wasEmpty) {
                            session.setState(GuildSession.SessionState.PLAYING);
                        }
                        
                        logger.info("Добавлено {} треков в очередь для гильдии {}", 
                                result.getTracks().size(), guildId);
                    }
                    return result;
                });
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Подключает бота к каналу и начинает воспроизведение.
     * Проверяет, что очередь не пуста перед началом.
     */
    @Override
    public void playInChannel(long guildId, String voiceChannelId) {
        GuildSession session = sessionRepository.getOrCreate(guildId);
        
        if (session.getQueue().isEmpty() && session.getQueue().getCurrentTrack().isEmpty()) {
            logger.warn("Попытка воспроизведения при пустой очереди для гильдии {}", guildId);
            return;
        }

        playerRegistry.connectToChannel(guildId, voiceChannelId);
        playerRegistry.startPlaying(guildId);
        
        session.setState(GuildSession.SessionState.PLAYING);
        logger.info("Начато воспроизведение в канале {} для гильдии {}", voiceChannelId, guildId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectToChannel(long guildId, String voiceChannelId) {
        playerRegistry.connectToChannel(guildId, voiceChannelId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Поток выполнения skip:
     * <ol>
     *     <li>Останавливает текущий трек в плеере</li>
     *     <li>Извлекает следующий трек из очереди</li>
     *     <li>Если трек найден — запускает его</li>
     *     <li>Если очередь пуста — сбрасывает состояние в IDLE</li>
     * </ol>
     */
    @Override
    public Optional<Track> skip(long guildId) {
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session == null) {
            return Optional.empty();
        }

        playerRegistry.stopCurrentTrack(guildId);
        
        // Запускаем следующий трек
        Optional<Track> nextTrack = session.getQueue().poll();
        if (nextTrack.isPresent()) {
            playerRegistry.playTrack(guildId, nextTrack.get());
            session.getQueue().setCurrentTrack(nextTrack.get());
            logger.debug("Пропущен трек, следующий: {}", nextTrack.get().getTitle());
        } else {
            session.getQueue().setCurrentTrack(null);
            session.setState(GuildSession.SessionState.IDLE);
            logger.debug("Очередь пуста после пропуска");
        }

        return nextTrack;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Полная остановка: очищает очередь, отключается от канала.
     */
    @Override
    public void stop(long guildId) {
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session == null) {
            return;
        }

        playerRegistry.stopCurrentTrack(guildId);
        session.getQueue().clear();
        session.getQueue().setCurrentTrack(null);
        session.setState(GuildSession.SessionState.IDLE);
        
        disconnect(guildId);
        logger.info("Воспроизведение остановлено для гильдии {}", guildId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean pause(long guildId) {
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session == null || session.getState() != GuildSession.SessionState.PLAYING) {
            return false;
        }

        playerRegistry.pause(guildId);
        session.setState(GuildSession.SessionState.PAUSED);
        logger.debug("Воспроизведение приостановлено для гильдии {}", guildId);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean resume(long guildId) {
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session == null || session.getState() != GuildSession.SessionState.PAUSED) {
            return false;
        }

        playerRegistry.resume(guildId);
        session.setState(GuildSession.SessionState.PLAYING);
        logger.debug("Воспроизведение возобновлено для гильдии {}", guildId);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVolume(long guildId, int volume) {
        int clampedVolume = Math.max(0, Math.min(100, volume));
        playerRegistry.setVolume(guildId, clampedVolume);
        logger.debug("Громкость установлена в {} для гильдии {}", clampedVolume, guildId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVolume(long guildId) {
        return playerRegistry.getVolume(guildId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shuffle(long guildId) {
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session == null) {
            return;
        }

        session.getQueue().shuffle();
        logger.info("Очередь перемешана для гильдии {}", guildId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Track> removeTrack(long guildId, int index) {
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session == null) {
            return Optional.empty();
        }

        Optional<Track> removed = session.getQueue().removeAt(index);
        removed.ifPresent(track -> 
            logger.debug("Удалён трек из очереди: {}", track.getTitle())
        );
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearQueue(long guildId) {
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session == null) {
            return;
        }

        session.getQueue().clear();
        logger.debug("Очередь очищена для гильдии {}", guildId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean toggleRepeat(long guildId) {
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session == null) {
            return false;
        }

        session.setRepeating(!session.isRepeating());
        playerRegistry.setRepeat(guildId, session.isRepeating());
        logger.debug("Повтор {} для гильдии {}", 
                    session.isRepeating() ? "включён" : "выключен", guildId);
        return session.isRepeating();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Track> getCurrentTrack(long guildId) {
        return sessionRepository.get(guildId)
                .map(session -> session.getQueue().getCurrentTrack().orElse(null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Track> getQueue(long guildId) {
        return sessionRepository.get(guildId)
                .map(session -> session.getQueue().getTracks())
                .orElse(List.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getQueueSize(long guildId) {
        return sessionRepository.get(guildId)
                .map(session -> session.getQueue().size())
                .orElse(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPlaying(long guildId) {
        return sessionRepository.get(guildId)
                .map(session -> session.getState() == GuildSession.SessionState.PLAYING)
                .orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect(long guildId) {
        playerRegistry.disconnect(guildId);
        GuildSession session = sessionRepository.get(guildId).orElse(null);
        if (session != null) {
            session.setVoiceChannel(null);
            session.setState(GuildSession.SessionState.IDLE);
        }
    }
}
