package by.example.MusicBot.infrastructure;

import by.example.MusicBot.domain.Track;
import by.example.MusicBot.service.TrackLoadResult;

import java.util.concurrent.CompletableFuture;

/**
 * Интерфейс для работы с аудио плеером.
 * 
 * <p>Этот интерфейс абстрагирует конкретную реализацию аудиоплеера
 * (Lavaplayer) от бизнес-логики. Это позволяет:
 * <ul>
 *     <li><b>Изолировать зависимости:</b> Service слой не зависит от Lavaplayer</li>
 *     <li><b>Заменять реализацию:</b> можно заменить Lavaplayer на другую библиотеку</li>
 *     <li><b>Упростить тестирование:</b> можно создать mock-реализацию для тестов</li>
 * </ul>
 * 
 * <h3>Архитектурное место:</h3>
 * <pre>
 * ┌─────────────────────┐
 * │ MusicServiceImpl    │ ← Service layer
 * └─────────┬───────────┘
 *           │ зависит от
 *           ▼
 * ┌─────────────────────┐
 * │ AudioPlayerRegistry │ ← Infrastructure (интерфейс)
 * └─────────┬───────────┘
 *           │ реализует
 *           ▼
 * ┌─────────────────────┐
 * │LavaplayerAudioPlayer│ ← Infrastructure (реализация)
 * └─────────────────────┘
 * </pre>
 * 
 * <h3>Основные возможности:</h3>
 * <ul>
 *     <li>Загрузка треков из различных источников (YouTube, SoundCloud, etc.)</li>
 *     <li>Подключение к голосовым каналам Discord</li>
 *     <li>Управление воспроизведением (play, pause, stop, skip)</li>
 *     <li>Контроль громкости и повтора</li>
 * </ul>
 * 
 * @see LavaplayerAudioPlayerRegistry
 */
public interface AudioPlayerRegistry {

    /**
     * Загружает трек по запросу (URL или поисковый запрос).
     * 
     * <p>Это асинхронная операция — возвращает CompletableFuture,
     * который завершится когда трек будет загружен из сети.
     * 
     * <p>Примеры запросов:
     * <ul>
     *     <li>"https://youtube.com/watch?v=..." — прямая ссылка</li>
     *     <li>"ytsearch:Never Gonna Give You Up" — поиск на YouTube</li>
     *     <li>"https://soundcloud.com/..." — ссылка на SoundCloud</li>
     * </ul>
     * 
     * @param query URL или поисковый запрос
     * @return CompletableFuture с результатом загрузки
     */
    CompletableFuture<TrackLoadResult> loadTrack(String query);

    /**
     * Подключается к указанному голосовому каналу Discord.
     * 
     * <p>Создаёт аудио-соединение между ботом и каналом,
     * позволяя передавать аудио-поток.
     * 
     * @param guildId ID гильдии (сервера Discord)
     * @param voiceChannelId ID голосового канала для подключения
     */
    void connectToChannel(long guildId, String voiceChannelId);

    /**
     * Начинает воспроизведение для указанной гильдии.
     * 
     * <p>Запускает первый трек из очереди воспроизведения.
     * Вызывается после подключения к каналу.
     * 
     * @param guildId ID гильдии
     */
    void startPlaying(long guildId);

    /**
     * Воспроизводит указанный трек.
     * 
     * <p>Передаёт трек в аудиоплеер для немедленного воспроизведения.
     * 
     * @param guildId ID гильдии
     * @param track трек для воспроизведения
     */
    void playTrack(long guildId, Track track);

    /**
     * Добавляет трек в очередь воспроизведения.
     * 
     * <p>В отличие от playTrack(), не начинает воспроизведение
     * немедленно, а добавляет трек в очередь на воспроизведение.
     * 
     * @param guildId ID гильдии
     * @param track трек для добавления в очередь
     */
    void queueTrack(long guildId, Track track);

    /**
     * Останавливает текущий воспроизводимый трек.
     * 
     * <p>Немедленно прекращает воспроизведение.
     * Следующий трек из очереди не запускается автоматически.
     * 
     * @param guildId ID гильдии
     */
    void stopCurrentTrack(long guildId);

    /**
     * Ставит воспроизведение на паузу.
     * 
     * <p>Приостанавливает текущий трек без изменения очереди.
     * Может быть возобновлено через resume().
     * 
     * @param guildId ID гильдии
     */
    void pause(long guildId);

    /**
     * Возобновляет воспроизведение после паузы.
     * 
     * @param guildId ID гильдии
     */
    void resume(long guildId);

    /**
     * Устанавливает громкость воспроизведения.
     * 
     * @param guildId ID гильдии
     * @param volume громкость от 0 (тихо) до 100 (максимум)
     */
    void setVolume(long guildId, int volume);

    /**
     * Возвращает текущую громкость.
     * 
     * @param guildId ID гильдии
     * @return текущая громкость от 0 до 100
     */
    int getVolume(long guildId);

    /**
     * Устанавливает режим повтора трека.
     * 
     * <p>В режиме повтора последний трек будет воспроизводиться
     * бесконечно.
     * 
     * @param guildId ID гильдии
     * @param repeat true для включения повтора, false для выключения
     */
    void setRepeat(long guildId, boolean repeat);

    /**
     * Отключается от голосового канала.
     * 
     * <p>Разрывает аудио-соединение с каналом.
     * Очередь треков сохраняется.
     * 
     * @param guildId ID гильдии
     */
    void disconnect(long guildId);
}
