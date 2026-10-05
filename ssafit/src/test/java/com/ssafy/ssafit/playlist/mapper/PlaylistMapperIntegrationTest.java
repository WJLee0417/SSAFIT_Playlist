package com.ssafy.ssafit.playlist.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ssafy.ssafit.mapper.DatabaseIntegrationTestSupport;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.music.domain.MusicSourceType;
import com.ssafy.ssafit.music.mapper.MusicMapper;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistSave;
import com.ssafy.ssafit.playlist.domain.PlaylistSourceType;
import com.ssafy.ssafit.playlist.domain.PlaylistType;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.domain.SpotifyExportStatus;

class PlaylistMapperIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private PlaylistItemMapper playlistItemMapper;

    @Autowired
    private PlaylistSaveMapper playlistSaveMapper;

    @Test
    void playlistMappersJoinVisibilityItemsSpotifyUrisAndSavedList() {
        Member owner = insertMember("playlist_owner");
        Member viewer = insertMember("playlist_viewer");
        Music spotifyMusic = insertMusic(
                "Spotify Join Song",
                "POP",
                150,
                MusicSourceType.SPOTIFY,
                "spotify:track:mapper-join");
        Music seedMusic = insertMusic(
                "Seed Join Song",
                "POP",
                140,
                MusicSourceType.SEED,
                null);
        Playlist publicPlaylist = insertPlaylist(
                owner.getId(),
                PlaylistType.USER,
                PlaylistSourceType.MANUAL,
                PlaylistVisibility.PUBLIC,
                "Public Join Playlist");
        insertPlaylist(
                owner.getId(),
                PlaylistType.USER,
                PlaylistSourceType.MANUAL,
                PlaylistVisibility.PRIVATE,
                "Private Join Playlist");
        insertItem(publicPlaylist.getId(), seedMusic.getId(), 2);
        insertItem(publicPlaylist.getId(), spotifyMusic.getId(), 1);

        PlaylistSave save = new PlaylistSave();
        save.setMemberId(viewer.getId());
        save.setPlaylistId(publicPlaylist.getId());
        playlistSaveMapper.insert(save);

        List<Playlist> viewerPlaylists =
                playlistMapper.selectList(viewer.getId(), null);
        assertThat(viewerPlaylists)
                .extracting(Playlist::getTitle)
                .contains("Public Join Playlist")
                .doesNotContain("Private Join Playlist");
        assertThat(viewerPlaylists.stream()
                .filter(playlist -> playlist.getId().equals(publicPlaylist.getId()))
                .findFirst()
                .orElseThrow()
                .isSaved()).isTrue();
        assertThat(playlistMapper.selectList(null, null).stream()
                .filter(playlist -> playlist.getId().equals(publicPlaylist.getId()))
                .findFirst()
                .orElseThrow()
                .isSaved()).isFalse();
        assertThat(playlistItemMapper.selectByPlaylistId(publicPlaylist.getId()))
                .extracting(PlaylistItem::getMusicId)
                .containsExactly(spotifyMusic.getId(), seedMusic.getId());
        assertThat(playlistItemMapper.selectSpotifyUrisByPlaylistId(publicPlaylist.getId()))
                .containsExactly("spotify:track:mapper-join");
        assertThat(playlistSaveMapper.selectSavedList(viewer.getId()))
                .allMatch(Playlist::isSaved)
                .extracting(Playlist::getTitle)
                .containsExactly("Public Join Playlist");
    }

    private Music insertMusic(
            String title,
            String genre,
            int bpm,
            MusicSourceType sourceType,
            String spotifyUri) {
        Music music = new Music();
        music.setTitle(title);
        music.setArtist("Mapper Test Artist");
        music.setGenre(genre);
        music.setBpm(bpm);
        music.setSourceType(sourceType);
        if (sourceType == MusicSourceType.SPOTIFY) {
            music.setSpotifyTrackId(title.toLowerCase().replace(' ', '-'));
            music.setSpotifyUri(spotifyUri);
            musicMapper.insertFromSpotify(music);
        } else {
            jdbcTemplate.update(
                    """
                    INSERT INTO music (title, artist, genre, bpm, source_type)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    music.getTitle(),
                    music.getArtist(),
                    music.getGenre(),
                    music.getBpm(),
                    music.getSourceType().name());
            music.setId(jdbcTemplate.queryForObject(
                    "SELECT id FROM music WHERE title = ?",
                    Long.class,
                    title));
        }
        return music;
    }

    private Playlist insertPlaylist(
            Long ownerId,
            PlaylistType type,
            PlaylistSourceType sourceType,
            PlaylistVisibility visibility,
            String title) {
        Playlist playlist = new Playlist();
        playlist.setOwnerId(ownerId);
        playlist.setPlaylistType(type);
        playlist.setSourceType(sourceType);
        playlist.setVisibility(visibility);
        playlist.setTitle(title);
        playlist.setSpotifyExportStatus(SpotifyExportStatus.NOT_EXPORTED);
        playlistMapper.insert(playlist);
        return playlist;
    }

    private void insertItem(Long playlistId, Long musicId, int orderNo) {
        PlaylistItem item = new PlaylistItem();
        item.setPlaylistId(playlistId);
        item.setMusicId(musicId);
        item.setOrderNo(orderNo);
        playlistItemMapper.insert(item);
    }
}
