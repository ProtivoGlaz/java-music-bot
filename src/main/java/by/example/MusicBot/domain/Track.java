package by.example.MusicBot.domain;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Domain-модель музыкального трека.
 * 
 * <p>Этот класс представляет собой обёртку над AudioTrack из Lavaplayer.
 * Он изолирует domain-слой от внешней библиотеки, что позволяет:
 * <ul>
 *     <li>Легче тестировать бизнес-логику</li>
 *     <li>Заменить Lavaplayer на другую библиотеку без изменения всего кода</li>
 *     <li>Иметь единый формат данных в приложении</li>
 * </ul>
 * 
 * <p>Пример использования:
 * <pre>{@code
 * Track track = new Track(audioTrack);
 * String title = track.getTitle();
 * String duration = track.getFormattedDuration();
 * }</pre>
 * 
 * @see AudioTrack
 */
public class Track {
    /** Уникальный идентификатор трека (выдаётся YouTube/другим источником) */
    private final String identifier;
    
    /** Название трека (например, "Never Gonna Give You Up") */
    private final String title;
    
    /** Автор/исполнитель (например, "Rick Astley") */
    private final String author;
    
    /** Длительность трека в миллисекундах */
    private final long duration;
    
    /** URL трека (прямая ссылка на YouTube или другой источник) */
    private final String uri;
    
    /** Флаг: является ли трек прямой трансляцией (стримом) */
    private final boolean isStream;
    
    /** 
     * Оригинальный AudioTrack из Lavaplayer.
     * Нужен для передачи в плеер при воспроизведении.
     */
    private final AudioTrack audioTrack;

    /**
     * Конструктор: создаёт Track из AudioTrack.
     * 
     * <p>Извлекает всю необходимую информацию из AudioTrack и сохраняет
     * для дальнейшего использования без зависимости от Lavaplayer.
     * 
     * @param audioTrack трек из Lavaplayer для обёртки
     */
    public Track(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
        this.identifier = audioTrack.getIdentifier();
        this.title = audioTrack.getInfo().title;
        this.author = audioTrack.getInfo().author;
        this.duration = audioTrack.getInfo().length;
        this.uri = audioTrack.getInfo().uri;
        this.isStream = audioTrack.getInfo().isStream;
    }

    /**
     * Возвращает уникальный идентификатор трека.
     * @return ID трека (например, YouTube video ID)
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Возвращает название трека.
     * @return название (например, "Never Gonna Give You Up")
     */
    public String getTitle() {
        return title;
    }

    /**
     * Возвращает автора/исполнителя трека.
     * @return имя исполнителя (например, "Rick Astley")
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Возвращает длительность трека в миллисекундах.
     * @return длительность в мс (например, 213000 = 3:33)
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Возвращает URL трека.
     * @return полная ссылка на трек
     */
    public String getUri() {
        return uri;
    }

    /**
     * Проверяет, является ли трек прямой трансляцией.
     * @return true если это стрим, false если обычное видео
     */
    public boolean isStream() {
        return isStream;
    }

    /**
     * Возвращает оригинальный AudioTrack.
     * Используется инфраструктурным слоем для воспроизведения.
     * 
     * @return AudioTrack из Lavaplayer
     */
    public AudioTrack getAudioTrack() {
        return audioTrack;
    }

    /**
     * Форматирует длительность трека в человекочитаемый вид.
     * 
     * <p>Примеры:
     * <ul>
     *     <li>213000 мс → "3:33"</li>
     *     <li>3661000 мс → "1:01:01"</li>
     * </ul>
     * 
     * @return отформатированная длительность (mm:ss или hh:mm:ss)
     */
    public String getFormattedDuration() {
        return formatDuration(duration);
    }

    /**
     * Вспомогательный метод для форматирования миллисекунд.
     * 
     * @param ms длительность в миллисекундах
     * @return строка формата mm:ss или hh:mm:ss
     */
    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            // Если есть часы, форматируем как hh:mm:ss
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        }
        // Иначе mm:ss
        return String.format("%d:%02d", minutes, seconds % 60);
    }

    /**
     * Возвращает строковое представление трека.
     * Используется для логирования и отладки.
     * 
     * @return строка вида "Автор - Название (длительность)"
     */
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", author, title, getFormattedDuration());
    }
}
