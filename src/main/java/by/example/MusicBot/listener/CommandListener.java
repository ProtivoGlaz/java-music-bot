package by.example.MusicBot.listener;

import by.example.MusicBot.domain.Track;
import by.example.MusicBot.service.MusicService;
import by.example.MusicBot.service.TrackLoadResult;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

/**
 * Presentation layer для обработки команд пользователя.
 * Слушает сообщения Discord и делегирует логику в MusicService.
 */
public class CommandListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CommandListener.class);
    private static final String PREFIX = "!";

    private final MusicService musicService;

    public CommandListener(MusicService musicService) {
        this.musicService = musicService;
        logger.info("CommandListener инициализирован");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw().trim();
        if (!content.startsWith(PREFIX)) {
            return;
        }

        String[] args = content.split("\\s+");
        String command = args[0].substring(PREFIX.length()).toLowerCase();
        Guild guild = event.getGuild();

        switch (command) {
            case "play" -> handlePlay(event, args, guild);
            case "stop" -> handleStop(event, guild);
            case "skip" -> handleSkip(event, guild);
            case "pause" -> handlePause(event, guild);
            case "resume" -> handleResume(event, guild);
            case "queue" -> handleQueue(event, guild);
            case "np" -> handleNowPlaying(event, guild);
            case "volume" -> handleVolume(event, args, guild);
            case "shuffle" -> handleShuffle(event, guild);
            case "clear" -> handleClear(event, guild);
            case "repeat" -> handleRepeat(event, guild);
            case "help" -> handleHelp(event);
            default -> {}
        }
    }

    private void handlePlay(MessageReceivedEvent event, String[] args, Guild guild) {
        if (args.length < 2) {
            sendError(event, "Использование: `!play <URL или название>`");
            return;
        }

        if (event.getMember() == null || event.getMember().getVoiceState() == null
                || event.getMember().getVoiceState().getChannel() == null) {
            sendError(event, "Вы должны находиться в голосовом канале");
            return;
        }

        String query = buildQuery(args);
        if (!query.startsWith("http://") && !query.startsWith("https://")) {
            query = "ytsearch:" + query;
        }

        long guildId = guild.getIdLong();
        String voiceChannelId = event.getMember().getVoiceState().getChannel().getId();
        String finalQuery = query;

        // Сначала подключаемся к голосовому каналу
        musicService.connectToChannel(guildId, voiceChannelId);

        // Затем загружаем и ставим в очередь трек
        musicService.loadAndQueue(guildId, finalQuery)
                .thenAccept(result -> {
                    switch (result.getStatus()) {
                        case TRACK_LOADED -> {
                            result.getFirstTrack().ifPresent(track -> 
                                sendEmbed(event, "Добавлено", "🎵 **" + track.getTitle() + "**", Color.GREEN)
                            );
                        }
                        case PLAYLIST_LOADED -> {
                            List<Track> tracks = result.getTracks();
                            sendEmbed(event, "Плейлист",
                                    "Добавлено **" + tracks.size() + "** треков", Color.BLUE);
                        }
                        case NOT_FOUND -> sendError(event, "Трек не найден: " + finalQuery);
                        case LOAD_FAILED -> sendError(event, "Ошибка: " + result.getErrorMessage());
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Ошибка при загрузке трека", ex);
                    sendError(event, "Произошла ошибка при загрузке трека");
                    return null;
                });
    }

    private void handleStop(MessageReceivedEvent event, Guild guild) {
        musicService.stop(guild.getIdLong());
        sendEmbed(event, "Остановлено", "Воспроизведение остановлено", Color.RED);
    }

    private void handleSkip(MessageReceivedEvent event, Guild guild) {
        Optional<Track> next = musicService.skip(guild.getIdLong());
        if (next.isPresent()) {
            sendEmbed(event, "Пропущено", "Следующий трек: **" + next.get().getTitle() + "**", Color.ORANGE);
        } else {
            sendEmbed(event, "Пропущено", "Очередь пуста", Color.YELLOW);
        }
    }

    private void handlePause(MessageReceivedEvent event, Guild guild) {
        if (musicService.pause(guild.getIdLong())) {
            sendEmbed(event, "Пауза", "Воспроизведение приостановлено", Color.YELLOW);
        } else {
            sendError(event, "Нечего ставить на паузу");
        }
    }

    private void handleResume(MessageReceivedEvent event, Guild guild) {
        if (musicService.resume(guild.getIdLong())) {
            sendEmbed(event, "Возобновлено", "Воспроизведение продолжено", Color.GREEN);
        } else {
            sendError(event, "Нечего возобновлять");
        }
    }

    private void handleQueue(MessageReceivedEvent event, Guild guild) {
        long guildId = guild.getIdLong();
        Optional<Track> current = musicService.getCurrentTrack(guildId);
        List<Track> queue = musicService.getQueue(guildId);

        if (current.isEmpty() && queue.isEmpty()) {
            sendEmbed(event, "Очередь пуста", "Сейчас ничего не играет", Color.YELLOW);
            return;
        }

        StringBuilder sb = new StringBuilder();

        current.ifPresent(track -> 
            sb.append("**Сейчас играет:**\n🎵 ").append(track.getTitle()).append("\n\n")
        );

        if (!queue.isEmpty()) {
            sb.append("**В очереди:**\n");
            for (int i = 0; i < Math.min(10, queue.size()); i++) {
                Track track = queue.get(i);
                sb.append(i + 1).append(". ").append(track.getTitle())
                        .append(" (").append(track.getFormattedDuration()).append(")\n");
            }
            if (queue.size() > 10) {
                sb.append("... и ещё ").append(queue.size() - 10).append(" треков");
            }
        }

        sendEmbed(event, "Очередь", sb.toString(), Color.BLUE);
    }

    private void handleNowPlaying(MessageReceivedEvent event, Guild guild) {
        long guildId = guild.getIdLong();
        Optional<Track> current = musicService.getCurrentTrack(guildId);

        if (current.isEmpty()) {
            sendError(event, "Сейчас ничего не играет. Используйте `!play`");
            return;
        }

        Track track = current.get();
        sendEmbed(event, "Сейчас играет", 
                "🎵 **" + track.getTitle() + "**\n" +
                "👤 " + track.getAuthor() + "\n" +
                "⏱️ " + track.getFormattedDuration(), 
                Color.BLUE);
    }

    private void handleVolume(MessageReceivedEvent event, String[] args, Guild guild) {
        if (args.length < 2) {
            int current = musicService.getVolume(guild.getIdLong());
            sendEmbed(event, "Громкость", "Текущая громкость: **" + current + "%**", Color.BLUE);
            return;
        }

        try {
            int volume = Integer.parseInt(args[1]);
            if (volume < 0 || volume > 100) {
                sendError(event, "Громкость должна быть от 0 до 100");
                return;
            }
            musicService.setVolume(guild.getIdLong(), volume);
            sendEmbed(event, "Громкость", "Установлено: **" + volume + "%**", Color.GREEN);
        } catch (NumberFormatException e) {
            sendError(event, "Неверный формат числа");
        }
    }

    private void handleShuffle(MessageReceivedEvent event, Guild guild) {
        musicService.shuffle(guild.getIdLong());
        sendEmbed(event, "Перемешано", "Очередь перемешана", Color.GREEN);
    }

    private void handleClear(MessageReceivedEvent event, Guild guild) {
        musicService.clearQueue(guild.getIdLong());
        sendEmbed(event, "Очищено", "Очередь очищена", Color.YELLOW);
    }

    private void handleRepeat(MessageReceivedEvent event, Guild guild) {
        boolean enabled = musicService.toggleRepeat(guild.getIdLong());
        sendEmbed(event, "Повтор", 
                enabled ? "Повтор включён 🔁" : "Повтор выключен ⏭️", 
                enabled ? Color.GREEN : Color.GRAY);
    }

    private void handleHelp(MessageReceivedEvent event) {
        String helpText = """
            **Команды бота:**
            `!play <URL/название>` — воспроизвести трек
            `!stop` — остановить и выйти
            `!skip` — пропустить трек
            `!pause` / `!resume` — пауза / возобновление
            `!volume <0-100>` — громкость
            `!queue` — показать очередь
            `!np` — текущий трек
            `!shuffle` — перемешать очередь
            `!clear` — очистить очередь
            `!repeat` — включить/выключить повтор
            `!help` — эта справка

            **Примеры:**
            `!play https://youtube.com/watch?v=...`
            `!play Never Gonna Give You Up`
            `!volume 50`
            """;
        sendEmbed(event, "Помощь", helpText, Color.MAGENTA);
    }

    private String buildQuery(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private void sendEmbed(MessageReceivedEvent event, String title, String description, Color color) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setFooter("MusicBot", null);

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendError(MessageReceivedEvent event, String message) {
        sendEmbed(event, "Ошибка", message, Color.RED);
    }
}
