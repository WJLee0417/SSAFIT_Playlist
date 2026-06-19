package com.ssafy.ssafit.playlist.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItem {

    private Long id;
    private Long playlistId;
    private Long musicId;
    private Integer orderNo;
    private LocalDateTime createdAt;
}
