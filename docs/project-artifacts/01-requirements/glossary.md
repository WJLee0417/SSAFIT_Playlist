# SSAFIT Playlist 용어집

본 용어집은 요구사항 정의서, Use Case Diagram, ER Diagram, WBS, 화면 설계서에서 같은 의미로 사용할 기준 용어를 정리한다.

| 용어 | 정의 | 구현 근거 |
| --- | --- | --- |
| Music | 서비스 내부에서 조회·추천·리뷰·플레이리스트 수록 대상으로 사용하는 음악 데이터이다. Spotify에서 가져온 곡과 내부 저장 곡을 모두 포함한다. | `Music`, `MusicApiController`, `MusicMapper`, `music` |
| Playlist | 여러 Music을 묶은 내부 플레이리스트이다. 사용자 생성, 공식, 추천 결과 저장본, Spotify export 상태를 포함한다. | `Playlist`, `PlaylistApiController`, `PlaylistMapper`, `playlists` |
| Playlist Item | Playlist 안에 들어가는 개별 수록곡 항목이다. 음악 ID와 순서 정보가 핵심이다. | `PlaylistItem`, `PlaylistItemApiController`, `PlaylistItemMapper`, `playlist_items` |
| Music Review | 회원이 특정 Music에 대해 선호도, 러닝 적합도, 코멘트를 남기는 평가 데이터이다. | `MusicReview`, `MusicReviewApiController`, `MusicReviewMapper`, `music_reviews` |
| Running Profile | 회원별 러닝 목표와 신체/운동 기준 정보를 저장하는 프로필이다. 목표 BPM, 목표 케이던스, 강도 계산에 사용된다. | `RunningProfile`, `RunningProfileApiController`, `RunningProfileViewController`, `running_profiles` |
| Running Session | 여러 회원이 함께 참여할 수 있는 러닝 모임이다. 작성자, 일정, 장소, 정원, 상태, 참여자 관계를 가진다. | `RunningSession`, `RunningSessionApiController`, `RunningSessionParticipantApiController`, `running_sessions` |
| Cadence | 러닝 중 분당 발걸음 수를 의미한다. 음악 BPM과 구분하기 위해 러닝 데이터에서는 평균 케이던스 또는 목표 케이던스 범위로 표현한다. | `average_cadence`, `target_cadence_min`, `target_cadence_max`, `CadenceResolutionService` |
| Heart Rate | 러닝 중 평균 심박수이다. 음악 BPM과 의미가 다르므로 `averageHeartRateBpm` 또는 `average_heart_rate_bpm`으로 구분한다. | `RunningRecord.averageHeartRateBpm`, `running_records.average_heart_rate_bpm` |
| Exercise Intensity | 러닝 운동 강도이다. 추천 조건과 운동 계획에서 LOW, MODERATE, HIGH, INTERVAL 같은 값으로 사용된다. | `RunningIntensity`, `RunningIntensityGoal`, `RunningIntensityResolutionService` |
| Recommendation | 러닝 프로필, 기록, 케이던스, 심박, BPM, 장르, 리뷰 등을 바탕으로 음악과 운동 계획을 제안하는 기능과 그 결과이다. | `RecommendationApiController`, `RecommendationService`, `recommendation_logs` |
| Spotify Track | Spotify Web API에서 식별되는 곡이다. 내부 DB에는 `spotifyTrackId`, `spotifyUri`, `spotifyExternalUrl` 등으로 저장된다. | `SpotifyTrack`, `SpotifyApiDtos.TrackResponse`, `music.spotify_track_id`, `music.spotify_uri` |
| Spotify OAuth | 회원이 Spotify 계정을 SSAFIT Playlist와 연결하기 위한 OAuth 인증 흐름이다. Access Token과 Refresh Token은 서버에서 관리하고 Vue에 노출하지 않는다. | `SpotifyOAuthService`, `SpotifyApiController`, `SpotifyConnectionMapper`, `spotify_connections` |
| Export | 내부 Playlist를 Spotify 계정의 플레이리스트로 생성하고 수록곡을 추가하는 기능이다. 결과는 내부 `playlists`의 Spotify export 상태 컬럼에 저장된다. | `PlaylistExportApiController`, `PlaylistExportService`, `SpotifyExportPanel.vue`, `playlists.spotify_export_status` |
| Internal Playlist | Spotify에 직접 존재하는 플레이리스트가 아니라 SSAFIT Playlist 서비스 DB에 저장된 Playlist이다. 수동 생성, 공식, 추천 저장본을 포함한다. | `PlaylistSourceType`, `PlaylistType`, `playlists`, `PlaylistService` |

## 상태 용어

| 상태 | 의미 |
| --- | --- |
| IMPLEMENTED | 현재 코드, 라우트, Mapper, 화면 또는 테스트 근거로 구현이 확인된 상태 |
| PARTIAL | 일부 경로는 구현되었지만 요구된 화면/흐름/연결이 일부 빠져 있는 상태 |
| PLANNED | 명세상 목표로 확인되지만 현재 구현 근거가 없는 계획 상태 |
| NOT_IMPLEMENTED | 요구사항으로 존재하지만 현재 코드에서 구현 근거가 없는 상태 |
| UNVERIFIED | 코드 구현은 있으나 외부 시스템, 권한, 런타임 환경, 수동 확인 없이는 완료를 확정할 수 없는 상태 |
