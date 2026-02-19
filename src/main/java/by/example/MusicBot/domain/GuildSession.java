package by.example.MusicBot.domain;

import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

/**
 * Domain-модель сессии гильдии (Discord-сервера).
 * 
 * <p>Хранит всё состояние музыкального бота для конкретного сервера:
 * <ul>
 *     <li>Очередь треков</li>
 *     <li>Текущий голосовой канал</li>
 *     <li>Состояние воспроизведения (играет/пауза/остановлено)</li>
 *     <li>Настройки (повтор трека)</li>
 * </ul>
 * 
 * <p>Каждая гильдия (сервер Discord) имеет свою сессию.
 * Это позволяет боту работать на нескольких серверах одновременно,
 * поддерживая независимые очереди и настройки для каждого.
 * 
 * <p>Пример жизненного цикла сессии:
 * <pre>{@code
 * 1. Пользователь вызывает !play → создаётся сессия
 * 2. Бот подключается к голосовому каналу
 * 3. Воспроизводится музыка
 * 4. Пользователь вызывает !stop → сессия сбрасывается
 * }</pre>
 */
public class GuildSession {
    /** 
     * Уникальный ID гильдии (сервера Discord).
     * Используется для идентификации сессии в репозитории.
     */
    private final long guildId;
    
    /** Очередь треков для этой гильдии */
    private final MusicQueue queue;
    
    /** 
     * Голосовой канал, к которому подключен бот.
     * null если бот не подключён.
     */
    private AudioChannel voiceChannel;
    
    /** 
     * Текущее состояние сессии.
     * Определяет, что делает бот в данный момент.
     */
    private SessionState state;
    
    /** 
     * Флаг повтора трека.
     * Если true, последний трек будет воспроизводиться бесконечно.
     */
    private boolean repeating;

    /**
     * Создаёт новую сессию для указанной гильдии.
     * 
     * <p>Инициализирует сессию в состоянии IDLE (бездействие)
     * с пустой очередью и выключенным повтором.
     * 
     * @param guildId ID гильдии (сервера Discord)
     */
    public GuildSession(long guildId) {
        this.guildId = guildId;
        this.queue = new MusicQueue();
        this.state = SessionState.IDLE;
        this.repeating = false;
    }

    /**
     * Возвращает ID гильдии.
     * 
     * @return уникальный ID сервера Discord
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Возвращает очередь треков этой гильдии.
     * 
     * <p>Через этот объект происходит управление очередью:
     * добавление треков, перемешивание, очистка и т.д.
     * 
     * @return объект MusicQueue для управления очередью
     */
    public MusicQueue getQueue() {
        return queue;
    }

    /**
     * Возвращает голосовой канал, к которому подключен бот.
     * 
     * @return голосовой канал или null если бот не подключён
     */
    public AudioChannel getVoiceChannel() {
        return voiceChannel;
    }

    /**
     * Устанавливает голосовой канал для подключения.
     * 
     * <p>Вызывается когда бот подключается к каналу
     * или отключается от него (устанавливается null).
     * 
     * @param voiceChannel канал для подключения или null
     */
    public void setVoiceChannel(AudioChannel voiceChannel) {
        this.voiceChannel = voiceChannel;
    }

    /**
     * Возвращает текущее состояние сессии.
     * 
     * @return одно из значений: IDLE, PLAYING, PAUSED
     */
    public SessionState getState() {
        return state;
    }

    /**
     * Устанавливает новое состояние сессии.
     * 
     * <p>Примеры перехода состояний:
     * <ul>
     *     <li>IDLE → PLAYING: когда начинается воспроизведение</li>
     *     <li>PLAYING → PAUSED: когда пользователь ставит на паузу</li>
     *     <li>PAUSED → PLAYING: когда возобновляется воспроизведение</li>
     *     <li>PLAYING → IDLE: когда очередь пуста</li>
     * </ul>
     * 
     * @param state новое состояние сессии
     */
    public void setState(SessionState state) {
        this.state = state;
    }

    /**
     * Проверяет, включён ли режим повтора.
     * 
     * @return true если повтор включён
     */
    public boolean isRepeating() {
        return repeating;
    }

    /**
     * Включает или выключает режим повтора.
     * 
     * <p>В режиме повтора последний воспроизводимый трек
     * будет играть бесконечно, пока пользователь не выключит повтор.
     * 
     * @param repeating true для включения повтора, false для выключения
     */
    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    /**
     * Проверяет, активна ли сессия.
     * 
     * <p>Сессия считается активной, если:
     * <ul>
     *     <li>Сейчас что-то воспроизводится (state != IDLE)</li>
     *     <li>ИЛИ в очереди есть треки</li>
     * </ul>
     * 
     * <p>Используется для определения, нужно ли хранить сессию
     * в памяти или можно удалить для экономии ресурсов.
     * 
     * @return true если сессия активна
     */
    public boolean isActive() {
        return state != SessionState.IDLE || !queue.isEmpty();
    }

    /**
     * Сбрасывает сессию в начальное состояние.
     * 
     * <p>Используется при остановке воспроизведения:
     * <ul>
     *     <li>Очищает очередь треков</li>
     *     <li>Сбрасывает голосовой канал</li>
     *     <li>Устанавливает состояние IDLE</li>
     *     <li>Выключает повтор</li>
     * </ul>
     * 
     * <p>После вызова этого метода сессия готова к новому использованию.
     */
    public void reset() {
        queue.clear();
        voiceChannel = null;
        state = SessionState.IDLE;
        repeating = false;
    }

    /**
     * Состояния сессии музыкального бота.
     * 
     * <p>Определяют текущий режим работы бота:
     * <ul>
     *     <li>IDLE — бот подключён, но ничего не играет</li>
     *     <li>PLAYING — бот воспроизводит музыку</li>
     *     <li>PAUSED — воспроизведение на паузе</li>
     * </ul>
     */
    public enum SessionState {
        /** Бездействие — бот подключён, но не воспроизводит */
        IDLE,
        
        /** Воспроизведение — музыка играет */
        PLAYING,
        
        /** Пауза — воспроизведение приостановлено */
        PAUSED
    }
}
