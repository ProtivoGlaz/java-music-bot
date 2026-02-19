package by.example.MusicBot.infrastructure;

import by.example.MusicBot.domain.Track;
import by.example.MusicBot.service.TrackLoadResult;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация AudioPlayerRegistry на основе Lavaplayer + JDA.
 * Инфраструктурный адаптер для работы с аудио.
 */
public class LavaplayerAudioPlayerRegistry implements AudioPlayerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(LavaplayerAudioPlayerRegistry.class);

    private final AudioPlayerManager playerManager;
    private final JDA jda;
    private final Map<Long, GuildAudioState> guildStates;

    public LavaplayerAudioPlayerRegistry(JDA jda) {
        this.jda = jda;
        this.playerManager = new DefaultAudioPlayerManager();
        this.guildStates = new ConcurrentHashMap<>();

        initializeSources();
        logger.info("LavaplayerAudioPlayerRegistry инициализирован");
    }

    private void initializeSources() {
        // Регистрируем YouTube источник первым
        YoutubeAudioSourceManager youtubeSource = new YoutubeAudioSourceManager();
        configureYouTube(youtubeSource);
        playerManager.registerSourceManager(youtubeSource);

        // Регистрируем остальные источники
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    private void configureYouTube(YoutubeAudioSourceManager youtubeSource) {
        String oauthToken = System.getenv("YOUTUBE_OAUTH_TOKEN");
        if (oauthToken != null && !oauthToken.isEmpty()) {
            try {
                youtubeSource.useOauth2(oauthToken, false);
                logger.info("YouTube OAuth настроен");
            } catch (Exception e) {
                logger.error("Ошибка настройки YouTube OAuth: {}", e.getMessage());
            }
        }
    }

    @Override
    public CompletableFuture<TrackLoadResult> loadTrack(String query) {
        CompletableFuture<TrackLoadResult> future = new CompletableFuture<>();

        playerManager.loadItemOrdered(this, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                logger.debug("Трек загружен: {}", track.getInfo().title);
                future.complete(TrackLoadResult.success(new Track(track)));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    AudioTrack first = playlist.getTracks().get(0);
                    future.complete(TrackLoadResult.success(new Track(first)));
                } else {
                    var tracks = playlist.getTracks().stream()
                            .map(Track::new)
                            .toList();
                    logger.debug("Плейлист загружен: {} треков", tracks.size());
                    future.complete(TrackLoadResult.playlist(tracks));
                }
            }

            @Override
            public void noMatches() {
                logger.debug("Трек не найден: {}", query);
                future.complete(TrackLoadResult.notFound());
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                logger.error("Ошибка загрузки трека {}: {}", query, exception.getMessage());
                future.complete(TrackLoadResult.error(exception.getMessage()));
            }
        });

        return future;
    }

    @Override
    public void connectToChannel(long guildId, String voiceChannelId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            logger.warn("Гильдия не найдена: {}", guildId);
            return;
        }

        AudioChannel channel = guild.getVoiceChannelById(voiceChannelId);
        if (channel == null) {
            logger.warn("Голосовой канал не найден: {}", voiceChannelId);
            return;
        }

        guild.getAudioManager().openAudioConnection(channel);
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(getPlayer(guildId)));
        
        logger.info("Подключено к голосовому каналу {} в гильдии {}", voiceChannelId, guildId);
    }

    @Override
    public void startPlaying(long guildId) {
        GuildAudioState state = getOrCreateState(guildId);
        state.startPlaying();
    }

    @Override
    public void playTrack(long guildId, Track track) {
        AudioPlayer player = getPlayer(guildId);
        player.startTrack(track.getAudioTrack(), false);
        logger.debug("Начато воспроизведение: {}", track.getTitle());
    }

    @Override
    public void queueTrack(long guildId, Track track) {
        GuildAudioState state = getOrCreateState(guildId);
        state.getScheduler().queue(track.getAudioTrack());
    }

    @Override
    public void stopCurrentTrack(long guildId) {
        AudioPlayer player = getPlayer(guildId);
        player.stopTrack();
        logger.debug("Текущий трек остановлен для гильдии {}", guildId);
    }

    @Override
    public void pause(long guildId) {
        AudioPlayer player = getPlayer(guildId);
        player.setPaused(true);
    }

    @Override
    public void resume(long guildId) {
        AudioPlayer player = getPlayer(guildId);
        player.setPaused(false);
    }

    @Override
    public void setVolume(long guildId, int volume) {
        AudioPlayer player = getPlayer(guildId);
        player.setVolume(volume);
    }

    @Override
    public int getVolume(long guildId) {
        AudioPlayer player = getPlayer(guildId);
        return player.getVolume();
    }

    @Override
    public void setRepeat(long guildId, boolean repeat) {
        GuildAudioState state = getOrCreateState(guildId);
        state.setRepeat(repeat);
    }

    @Override
    public void disconnect(long guildId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild != null) {
            guild.getAudioManager().closeAudioConnection();
            guildStates.remove(guildId);
            logger.info("Отключено от голосового канала в гильдии {}", guildId);
        }
    }

    private AudioPlayer getPlayer(long guildId) {
        return getOrCreateState(guildId).getPlayer();
    }

    private GuildAudioState getOrCreateState(long guildId) {
        return guildStates.computeIfAbsent(guildId, id -> {
            AudioPlayer player = playerManager.createPlayer();
            return new GuildAudioState(player);
        });
    }

    /**
     * Внутреннее состояние для гильдии.
     */
    static class GuildAudioState {
        private final AudioPlayer player;
        private final TrackScheduler scheduler;
        private final boolean[] repeat;

        public GuildAudioState(AudioPlayer player) {
            this.player = player;
            this.repeat = new boolean[]{false};
            this.scheduler = new TrackScheduler(player, repeat);
            player.addListener(scheduler);
        }

        public AudioPlayer getPlayer() {
            return player;
        }

        public TrackScheduler getScheduler() {
            return scheduler;
        }

        public boolean isRepeat() {
            return repeat[0];
        }

        public void setRepeat(boolean repeat) {
            this.repeat[0] = repeat;
        }

        public void startPlaying() {
            scheduler.playNext();
        }
    }
}
