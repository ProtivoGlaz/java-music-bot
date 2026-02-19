package by.example.MusicBot.service;

import by.example.MusicBot.domain.Track;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис управления музыкой — интерфейс для бизнес-логики бота.
 * 
 * <p>Этот интерфейс определяет все операции, которые может выполнять
 * музыкальный бот. Он представляет собой контракт между:
 * <ul>
 *     <li><b>Presentation layer</b> (CommandListener) — вызывает методы сервиса</li>
 *     <li><b>Service layer</b> (MusicServiceImpl) — реализует логику</li>
 * </ul>
 * 
 * <h3>Основные возможности:</h3>
 * <ul>
 *     <li>Загрузка и воспроизведение треков</li>
 *     <li>Управление очередью (добавление, удаление, перемешивание)</li>
 *     <li>Контроль воспроизведения (пауза, повтор, громкость)</li>
 *     <li>Получение информации о текущем треке и очереди</li>
 * </ul>
 * 
 * <h3>Пример использования:</h3>
 * <pre>{@code
 * // В CommandListener при команде !play
 * musicService.loadAndQueue(guildId, "Never Gonna Give You Up")
 *     .thenAccept(result -> {
 *         if (result.getStatus() == TRACK_LOADED) {
 *             sendEmbed("Добавлено: " + result.getFirstTrack().getTitle());
 *         }
 *     });
 * }</pre>
 * 
 * @see MusicServiceImpl
 */
public interface MusicService {

    /**
     * Загружает трек по запросу и добавляет в очередь.
     * 
     * <p>Это асинхронная операция — загрузка трека из YouTube/другого
     * источника требует времени и сетевого запроса.
     * 
     * <p>Пример запроса:
     * <ul>
     *     <li>URL: "https://youtube.com/watch?v=dQw4w9WgXcQ"</li>
     *     <li>Поиск: "ytsearch:Never Gonna Give You Up"</li>
     *     <li>Плейлист: "https://youtube.com/playlist?list=..."</li>
     * </ul>
     * 
     * @param guildId ID гильдии (сервера Discord)
     * @param query URL или поисковый запрос
     * @return CompletableFuture с результатом загрузки
     */
    CompletableFuture<TrackLoadResult> loadAndQueue(long guildId, String query);

    /**
     * Начинает воспроизведение в указанном голосовом канале.
     * 
     * <p>Подключает бота к каналу и начинает воспроизведение
     * первого трека из очереди.
     * 
     * @param guildId ID гильдии
     * @param voiceChannelId ID голосового канала для подключения
     */
    void playInChannel(long guildId, String voiceChannelId);

    /**
     * Подключается к голосовому каналу без начала воспроизведения.
     * 
     * <p>Используется когда нужно заранее подключить бота к каналу
     * перед загрузкой трека.
     * 
     * @param guildId ID гильдии
     * @param voiceChannelId ID голосового канала
     */
    void connectToChannel(long guildId, String voiceChannelId);

    /**
     * Пропускает текущий трек.
     * 
     * <p>Останавливает текущее воспроизведение и начинает следующий
     * трек из очереди. Если очередь пуста, останавливает воспроизведение.
     * 
     * @param guildId ID гильдии
     * @return следующий трек или пустой Optional если очередь пуста
     */
    Optional<Track> skip(long guildId);

    /**
     * Останавливает воспроизведение и очищает очередь.
     * 
     * <p>Полная остановка бота:
     * <ul>
     *     <li>Останавливает текущий трек</li>
     *     <li>Очищает очередь</li>
     *     <li>Отключается от голосового канала</li>
     * </ul>
     * 
     * @param guildId ID гильдии
     */
    void stop(long guildId);

    /**
     * Ставит воспроизведение на паузу.
     * 
     * <p>Приостанавливает текущий трек без изменения очереди.
     * Может быть возобновлено через {@link #resume(long)}.
     * 
     * @param guildId ID гильдии
     * @return true если успешно поставлено на паузу
     */
    boolean pause(long guildId);

    /**
     * Возобновляет воспроизведение после паузы.
     * 
     * @param guildId ID гильдии
     * @return true если успешно возобновлено
     */
    boolean resume(long guildId);

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
     * @return громкость от 0 до 100
     */
    int getVolume(long guildId);

    /**
     * Перемешивает очередь треков.
     * 
     * <p>Случайным образом меняет порядок треков в очереди.
     * Текущий воспроизводимый трек не затрагивается.
     * 
     * @param guildId ID гильдии
     */
    void shuffle(long guildId);

    /**
     * Удаляет трек из очереди по индексу.
     * 
     * @param guildId ID гильдии
     * @param index индекс трека (0-based, считая от начала очереди)
     * @return удалённый трек или пустой Optional если индекс неверный
     */
    Optional<Track> removeTrack(long guildId, int index);

    /**
     * Очищает очередь треков.
     * 
     * <p>Удаляет все треки из очереди, но не останавливает
     * текущее воспроизведение.
     * 
     * @param guildId ID гильдии
     */
    void clearQueue(long guildId);

    /**
     * Включает или выключает режим повтора.
     * 
     * <p>В режиме повтора последний воспроизводимый трек
     * будет играть бесконечно.
     * 
     * @param guildId ID гильдии
     * @return новое состояние повтора (true = включён)
     */
    boolean toggleRepeat(long guildId);

    /**
     * Возвращает текущий воспроизводимый трек.
     * 
     * @param guildId ID гильдии
     * @return текущий трек или пустой Optional если ничего не играет
     */
    Optional<Track> getCurrentTrack(long guildId);

    /**
     * Возвращает список треков в очереди.
     * 
     * @param guildId ID гильдии
     * @return список треков (не включает текущий)
     */
    List<Track> getQueue(long guildId);

    /**
     * Возвращает размер очереди.
     * 
     * @param guildId ID гильдии
     * @return количество треков в очереди
     */
    int getQueueSize(long guildId);

    /**
     * Проверяет, играет ли что-то сейчас.
     * 
     * @param guildId ID гильдии
     * @return true если воспроизведение активно
     */
    boolean isPlaying(long guildId);

    /**
     * Отключается от голосового канала.
     * 
     * <p>Разрывает соединение с голосовым каналом,
     * но не очищает очередь и не меняет состояние.
     * 
     * @param guildId ID гильдии
     */
    void disconnect(long guildId);
}
