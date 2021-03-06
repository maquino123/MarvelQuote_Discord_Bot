package events.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    private PlayerManager(){
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        //register from remote and local sources
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public synchronized GuildMusicManager getGuildMusicManager(Guild guild){
        long guildID = guild.getIdLong();

        //Get the music manager
        GuildMusicManager musicManager = musicManagers.get(guildID);

        //Creates new music manager if none
        if (musicManager == null){
            musicManager  = new GuildMusicManager(playerManager);
            musicManagers.put(guildID, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    public void loadAndPlay(final TextChannel channel, final String trackUrl){
        final GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                channel.sendMessage("Adding to queue: " + audioTrack.getInfo().title).queue();

                play(musicManager, audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                //AudioTrack firstTrack = audioPlaylist.getSelectedTrack();
                Random random = new Random();

                //if (firstTrack == null) {
                //    firstTrack = audioPlaylist.getTracks().remove(0);
                //}

                channel.sendMessage("Found a playlist!: " + audioPlaylist.getName()).queue();
                //int randomIndex = random.nextInt(audioPlaylist.getTracks().size());

                //play(musicManager, firstTrack);

                //All tracks added to queue
                //audioPlaylist.getTracks().forEach(musicManager.scheduler::queue);
                //play(musicManager, track);

                for (int i = 0; i < audioPlaylist.getTracks().size(); i++) {
                    int randomIndex = random.nextInt(audioPlaylist.getTracks().size());
                    AudioTrack randomTrack = audioPlaylist.getTracks().get(randomIndex);
                    play(musicManager, randomTrack);
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                channel.sendMessage("Could not play " + e.getMessage()).queue();
            }
        });
    }

    private void play(GuildMusicManager musicManager, AudioTrack track){
        musicManager.scheduler.queue(track);
    }

    public static synchronized  PlayerManager getInstance() {
        if (INSTANCE == null){
            INSTANCE = new PlayerManager();
        }

        return INSTANCE;
    }
}
