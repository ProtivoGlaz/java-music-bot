package by.example.MusicBot.service;

import by.example.MusicBot.domain.Track;

import java.util.List;
import java.util.Optional;

/**
 * Результат загрузки трека из музыкального источника.
 * 
 * <p>Этот класс инкапсулирует результат операции загрузки трека,
 * который может быть:
 * <ul>
 *     <li>Успешная загрузка одного трека</li>
 *     <li>Загрузка плейлиста (несколько треков)</li>
 *     <li>Трек не найден</li>
 *     <li>Ошибка загрузки</li>
 * </ul>
 * 
 * <p>Использует паттерн "Result Object" для безопасной передачи
 * результатов между слоями приложения.
 * 
 * <h3>Пример использования:</h3>
 * <pre>{@code
 * TrackLoadResult result = loadTrack(query);
 * if (result.getStatus() == LoadStatus.TRACK_LOADED) {
 *     Track track = result.getFirstTrack().get();
 *     // Воспроизводим трек
 * }
 * }</pre>
 * 
 * @see LoadStatus
 */
public class TrackLoadResult {
    /** Статус операции загрузки */
    private final LoadStatus status;
    
    /** Список загруженных треков (может содержать 0 или более треков) */
    private final List<Track> tracks;
    
    /** Сообщение об ошибке (null если ошибок не было) */
    private final String errorMessage;

    /**
     * Приватный конструктор для создания через статические фабрики.
     * 
     * @param status статус операции
     * @param tracks список треков
     * @param errorMessage сообщение об ошибке или null
     */
    private TrackLoadResult(LoadStatus status, List<Track> tracks, String errorMessage) {
        this.status = status;
        this.tracks = tracks;
        this.errorMessage = errorMessage;
    }

    /**
     * Создаёт результат успешной загрузки одного трека.
     * 
     * @param track загруженный трек
     * @return результат со статусом TRACK_LOADED
     */
    public static TrackLoadResult success(Track track) {
        return new TrackLoadResult(LoadStatus.TRACK_LOADED, List.of(track), null);
    }

    /**
     * Создаёт результат загрузки плейлиста.
     * 
     * @param tracks список треков из плейлиста
     * @return результат со статусом PLAYLIST_LOADED
     */
    public static TrackLoadResult playlist(List<Track> tracks) {
        return new TrackLoadResult(LoadStatus.PLAYLIST_LOADED, tracks, null);
    }

    /**
     * Создаёт результат "трек не найден".
     * 
     * @return результат со статусом NOT_FOUND
     */
    public static TrackLoadResult notFound() {
        return new TrackLoadResult(LoadStatus.NOT_FOUND, List.of(), "Трек не найден");
    }

    /**
     * Создаёт результат ошибки загрузки.
     * 
     * @param message описание ошибки
     * @return результат со статусом LOAD_FAILED
     */
    public static TrackLoadResult error(String message) {
        return new TrackLoadResult(LoadStatus.LOAD_FAILED, List.of(), message);
    }

    /**
     * Возвращает статус операции загрузки.
     * 
     * @return один из LoadStatus
     */
    public LoadStatus getStatus() {
        return status;
    }

    /**
     * Возвращает все загруженные треки.
     * 
     * @return список треков (может быть пустым)
     */
    public List<Track> getTracks() {
        return tracks;
    }

    /**
     * Возвращает первый трек из результата.
     * 
     * <p>Удобный метод для случаев, когда ожидается один трек:
     * <ul>
     *     <li>При загрузке по URL — вернёт этот трек</li>
     *     <li>При поиске — вернёт первый результат поиска</li>
     *     <li>При ошибке — вернёт пустой Optional</li>
     * </ul>
     * 
     * @return Optional с первым треком или пустой Optional
     */
    public Optional<Track> getFirstTrack() {
        return tracks.isEmpty() ? Optional.empty() : Optional.of(tracks.get(0));
    }

    /**
     * Возвращает сообщение об ошибке.
     * 
     * @return описание ошибки или null если ошибок не было
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Статусы операции загрузки трека.
     * 
     * <p>Определяют, что произошло при попытке загрузки:
     * <ul>
     *     <li>TRACK_LOADED — загружен один трек</li>
     *     <li>PLAYLIST_LOADED — загружен плейлист (несколько треков)</li>
     *     <li>NOT_FOUND — трек не найден в источнике</li>
     *     <li>LOAD_FAILED — произошла ошибка при загрузке</li>
     * </ul>
     */
    public enum LoadStatus {
        /** Успешно загружен один трек */
        TRACK_LOADED,
        
        /** Успешно загружен плейлист */
        PLAYLIST_LOADED,
        
        /** Трек не найден */
        NOT_FOUND,
        
        /** Произошла ошибка при загрузке */
        LOAD_FAILED
    }
}
