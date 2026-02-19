package by.example.MusicBot.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Domain-модель очереди воспроизведения.
 * 
 * <p>Управляет коллекцией треков, ожидающих воспроизведения.
 * Реализует потокобезопасную очередь с поддержкой операций:
 * <ul>
 *     <li>Добавление треков</li>
 *     <li>Извлечение следующего трека</li>
 *     <li>Перемешивание</li>
 *     <li>Удаление по индексу</li>
 * </ul>
 * 
 * <p>Использует {@link ConcurrentLinkedQueue} для потокобезопасности,
 * так как доступ к очереди может происходить из разных потоков:
 * - Поток JDA (обработка команд пользователя)
 * - Поток Lavaplayer (воспроизведение треков)
 * 
 * <p>Пример использования:
 * <pre>{@code
 * MusicQueue queue = new MusicQueue();
 * queue.add(track1);
 * queue.add(track2);
 * Optional<Track> next = queue.poll(); // Получит track1
 * }</pre>
 */
public class MusicQueue {
    /** 
     * Очередь треков, ожидающих воспроизведения.
     * Используется ConcurrentLinkedQueue для потокобезопасности.
     */
    private final Queue<Track> queue;
    
    /** 
     * Трек, который сейчас воспроизводится.
     * null если ничего не играет.
     */
    private Track currentTrack;
    
    /** Флаг: перемешана ли очередь */
    private boolean shuffled;

    /**
     * Создаёт новую пустую очередь.
     */
    public MusicQueue() {
        this.queue = new ConcurrentLinkedQueue<>();
        this.shuffled = false;
    }

    /**
     * Добавляет трек в конец очереди.
     * 
     * <p>Поток добавления:
     * 1. Пользователь вызывает !play
     * 2. Трек загружается и добавляется в очередь
     * 3. Если очередь была пустой, трек начинает воспроизводиться
     * 
     * @param track трек для добавления в очередь
     */
    public synchronized void add(Track track) {
        queue.offer(track);
    }

    /**
     * Добавляет коллекцию треков в очередь.
     * 
     * <p>Используется при загрузке плейлистов — все треки
     * добавляются разом вместо поочерёдного добавления.
     * 
     * @param tracks список треков для добавления
     */
    public synchronized void addAll(List<Track> tracks) {
        tracks.forEach(this::add);
    }

    /**
     * Возвращает следующий трек из очереди без удаления.
     * 
     * <p>Используется для отображения текущего трека в очереди,
     * который начнёт воспроизводиться следующим.
     * 
     * @return Optional с следующим треком или пустой Optional
     */
    public synchronized Optional<Track> peek() {
        return Optional.ofNullable(queue.peek());
    }

    /**
     * Извлекает следующий трек из очереди.
     * 
     * <p>Основной метод для получения трека для воспроизведения.
     * Трек удаляется из очереди после извлечения.
     * 
     * <p>Поток выполнения:
     * 1. Текущий трек завершается
     * 2. Вызывается poll() для получения следующего
     * 3. Если трек найден, он начинает воспроизводиться
     * 
     * @return Optional с следующим треком или пустой Optional
     */
    public synchronized Optional<Track> poll() {
        return Optional.ofNullable(queue.poll());
    }

    /**
     * Устанавливает текущий воспроизводимый трек.
     * 
     * <p>Вызывается инфраструктурным слоем, когда начинается
     * воспроизведение нового трека.
     * 
     * @param track текущий трек или null если ничего не играет
     */
    public synchronized void setCurrentTrack(Track track) {
        this.currentTrack = track;
    }

    /**
     * Возвращает текущий воспроизводимый трек.
     * 
     * @return Optional с текущим треком или пустой Optional
     */
    public synchronized Optional<Track> getCurrentTrack() {
        return Optional.ofNullable(currentTrack);
    }

    /**
     * Возвращает количество треков в очереди.
     * 
     * @return размер очереди (не включает текущий трек)
     */
    public synchronized int size() {
        return queue.size();
    }

    /**
     * Проверяет, пуста ли очередь.
     * 
     * @return true если в очереди нет треков
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Очищает очередь.
     * 
     * <p>Используется командой !clear или при остановке бота.
     * Текущий трек НЕ очищается — для этого есть отдельный метод.
     */
    public synchronized void clear() {
        queue.clear();
        shuffled = false;
    }

    /**
     * Перемешивает очередь случайным образом.
     * 
     * <p>Алгоритм:
     * 1. Копирует все треки в LinkedList
     * 2. Перемешивает Collections.shuffle()
     * 3. Очищает оригинальную очередь
     * 4. Добавляет перемешанные треки обратно
     * 
     * <p>Используется командой !shuffle
     */
    public synchronized void shuffle() {
        List<Track> tracks = new LinkedList<>(queue);
        java.util.Collections.shuffle(tracks);
        queue.clear();
        queue.addAll(tracks);
        shuffled = true;
    }

    /**
     * Удаляет трек по указанному индексу.
     * 
     * <p>Индексы начинаются с 0. Например:
     * <ul>
     *     <li>0 — первый трек в очереди</li>
     *     <li>1 — второй трек</li>
     * </ul>
     * 
     * @param index индекс трека (0-based)
     * @return Optional с удалённым треком или пустой Optional если индекс неверный
     */
    public synchronized Optional<Track> removeAt(int index) {
        if (index < 0 || index >= queue.size()) {
            return Optional.empty();
        }
        List<Track> tracks = new LinkedList<>(queue);
        Track removed = tracks.remove(index);
        queue.clear();
        queue.addAll(tracks);
        return Optional.of(removed);
    }

    /**
     * Возвращает список всех треков в очереди.
     * 
     * <p>Возвращает копию списка, чтобы изменения в возвращённом
     * списке не влияли на оригинальную очередь.
     * 
     * @return копия списка треков
     */
    public synchronized List<Track> getTracks() {
        return new LinkedList<>(queue);
    }

    /**
     * Возвращает первые N треков из очереди.
     * 
     * <p>Используется для отображения очереди в Discord,
     * чтобы не показывать слишком много треков сразу.
     * 
     * @param limit максимальное количество треков
     * @return список из первых limit треков
     */
    public synchronized List<Track> getTracks(int limit) {
        return getTracks().stream().limit(limit).toList();
    }

    /**
     * Проверяет, была ли очередь перемешана.
     * 
     * @return true если очередь перемешана
     */
    public boolean isShuffled() {
        return shuffled;
    }
}
