package com.tappazo.infrastructure.external;

import com.tappazo.application.port.out.VoiceProviderPort.VoiceToken;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiveKitRoomAdapter Unit Tests")
class LiveKitRoomAdapterTest {

    @Mock
    private RoomServiceClient roomServiceClient;

    private LiveKitConfig liveKitConfig;
    private LiveKitRoomAdapter liveKitAdapter;

    @BeforeEach
    void setUp() {
        liveKitConfig = new LiveKitConfig();
        ReflectionTestUtils.setField(liveKitConfig, "apiKey", "devkey");
        ReflectionTestUtils.setField(liveKitConfig, "apiSecret", "secret1234567890secret1234567890");
        ReflectionTestUtils.setField(liveKitConfig, "serverUrl", "ws://localhost:7880");

        liveKitAdapter = new LiveKitRoomAdapter(liveKitConfig, roomServiceClient);
    }

    @Test
    @DisplayName("generateToken returns valid VoiceToken record with roomName and serverUrl")
    void generateToken_basic_success() {
        VoiceToken voiceToken = liveKitAdapter.generateToken("game-123", "user-456");

        assertThat(voiceToken).isNotNull();
        assertThat(voiceToken.roomName()).isEqualTo("game-game-123");
        assertThat(voiceToken.serverUrl()).isEqualTo("ws://localhost:7880");
        assertThat(voiceToken.token()).isNotBlank();
        // JWT typically contains 3 segments separated by dots
        assertThat(voiceToken.token().split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("generateToken with custom parameters generates proper token")
    void generateToken_withCustomParams_success() {
        VoiceToken voiceToken = liveKitAdapter.generateToken(
                "game-abc",
                "user-xyz",
                "PlayerOne",
                true,
                true,
                Duration.ofHours(1)
        );

        assertThat(voiceToken.roomName()).isEqualTo("game-game-abc");
        assertThat(voiceToken.token()).isNotBlank();
    }

    @Test
    @DisplayName("createRoom delegates to RoomServiceClient")
    void createRoom_delegatesToClient() throws Exception {
        @SuppressWarnings("unchecked")
        Call<LivekitModels.Room> mockCall = mock(Call.class);
        when(roomServiceClient.createRoom(anyString())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(Response.success(null));

        liveKitAdapter.createRoom("game-123");

        verify(roomServiceClient).createRoom("game-123");
        verify(mockCall).execute();
    }

    @Test
    @DisplayName("deleteRoom delegates to RoomServiceClient")
    void deleteRoom_delegatesToClient() throws Exception {
        @SuppressWarnings("unchecked")
        Call<Void> mockCall = mock(Call.class);
        when(roomServiceClient.deleteRoom(anyString())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(Response.success(null));

        liveKitAdapter.deleteRoom("game-123");

        verify(roomServiceClient).deleteRoom("game-123");
        verify(mockCall).execute();
    }

    @Test
    @DisplayName("listParticipants delegates to RoomServiceClient")
    void listParticipants_delegatesToClient() throws Exception {
        @SuppressWarnings("unchecked")
        Call<List<LivekitModels.ParticipantInfo>> mockCall = mock(Call.class);
        when(roomServiceClient.listParticipants(anyString())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(Response.success(List.of()));

        List<LivekitModels.ParticipantInfo> participants = liveKitAdapter.listParticipants("game-123");

        assertThat(participants).isEmpty();
        verify(roomServiceClient).listParticipants("game-123");
    }

    @Test
    @DisplayName("LiveKitConfig toHttpUrl transforms ws/wss protocols to http/https")
    void toHttpUrl_transformsCorrectly() {
        assertThat(LiveKitConfig.toHttpUrl("ws://localhost:7880")).isEqualTo("http://localhost:7880");
        assertThat(LiveKitConfig.toHttpUrl("wss://livekit.example.com")).isEqualTo("https://livekit.example.com");
        assertThat(LiveKitConfig.toHttpUrl("http://localhost:7880")).isEqualTo("http://localhost:7880");
        assertThat(LiveKitConfig.toHttpUrl(null)).isEqualTo("http://localhost:7880");
    }
}
