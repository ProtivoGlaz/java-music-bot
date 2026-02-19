package by.example.MusicBot.infrastructure;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Планировщик треков для Lavaplayer.
 * Обрабатывает события плеера и управляет очередью.
 */
class TrackScheduler extends AudioEventAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);

    private final AudioPlayer player;
    private final boolean[] repeat;  // shared flag
    private final Queue<AudioTrack> queue;
    private AudioTrack lastTrack;  // последний воспроизводимый трек

    public TrackScheduler(AudioPlayer player, boolean[] repeat) {
        this.player = player;
        this.repeat = repeat;
        this.queue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Добавить трек в очередь.
     */
    public synchronized void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
            logger.info("Добавлено в очередь: {}", track.getInfo().title);
        } else {
            lastTrack = track;
            logger.info("Начато воспроизведение: {}", track.getInfo().title);
        }
    }

    /**
     * Запустить следующий трек.
     */
    public synchronized void playNext() {
        if (repeat[0] && lastTrack != null) {
            // Повтор последнего трека
            player.startTrack(lastTrack.makeClone(), false);
            logger.debug("Повтор трека: {}", lastTrack.getInfo().title);
            return;
        }

        AudioTrack next = queue.poll();
        if (next != null) {
            lastTrack = next;
            player.startTrack(next, false);
            logger.info("Следующий трек: {}", next.getInfo().title);
        } else {
            lastTrack = null;
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            playNext();
        }
    }

    public void onTrackException(AudioPlayer player, AudioTrack track, Throwable throwable) {
        logger.error("Ошибка воспроизведения трека: {}", track.getInfo().title, throwable);
    }

    public Queue<AudioTrack> getQueue() {
        return queue;
    }

    public synchronized void clearQueue() {
        queue.clear();
    }
}
