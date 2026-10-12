import { Room, RoomEvent, Track } from 'livekit-client';
import './style.css';

const form = document.querySelector('#connection-form');
const serverUrlInput = document.querySelector('#server-url');
serverUrlInput.value = `wss://${window.location.host}/livekit`;
const tokenInput = document.querySelector('#voice-token');
const connectButton = document.querySelector('#connect-button');
const disconnectButton = document.querySelector('#disconnect-button');
const statusHeading = document.querySelector('#status');
const statusIndicator = document.querySelector('#status-indicator');
const connectionDetail = document.querySelector('#connection-detail');
const eventsList = document.querySelector('#events');
const remoteAudio = document.querySelector('#remote-audio');

let room;

function logEvent(message) {
  const item = document.createElement('li');
  item.textContent = `${new Date().toLocaleTimeString()} — ${message}`;
  eventsList.prepend(item);
}

function setStatus(message, connected = false) {
  statusHeading.textContent = message;
  statusIndicator.classList.toggle('connected', connected);
}

function detachAudio(track) {
  track.detach().forEach((element) => element.remove());
}

disconnectButton.addEventListener('click', async () => {
  disconnectButton.disabled = true;
  if (room) {
    await room.disconnect();
  }
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  connectButton.disabled = true;
  setStatus('Conectando…');
  connectionDetail.textContent = '';

  try {
    room = new Room({
      adaptiveStream: true,
      dynacast: true,
    });

    room
      .on(RoomEvent.Connected, () => {
        setStatus('Conectado', true);
        connectionDetail.textContent = `Sala: ${room.name} · Participante: ${room.localParticipant.identity}`;
        logEvent(`Conectado a ${room.name}`);
      })
      .on(RoomEvent.Disconnected, (reason) => {
        setStatus('Desconectado');
        connectionDetail.textContent = reason ? `Motivo: ${reason}` : 'La conexión se cerró.';
        logEvent('Desconectado de la sala');
        connectButton.disabled = false;
        disconnectButton.disabled = true;
        remoteAudio.replaceChildren();
      })
      .on(RoomEvent.ParticipantConnected, (participant) => {
        logEvent(`${participant.identity} se unió a la sala`);
      })
      .on(RoomEvent.ParticipantDisconnected, (participant) => {
        logEvent(`${participant.identity} salió de la sala`);
      })
      .on(RoomEvent.TrackSubscribed, (track, publication, participant) => {
        if (track.kind === Track.Kind.Audio) {
          const audioElement = track.attach();
          audioElement.autoplay = true;
          audioElement.dataset.trackSid = publication.trackSid;
          remoteAudio.append(audioElement);
          logEvent(`Audio recibido de ${participant.identity}`);
        }
      })
      .on(RoomEvent.TrackUnsubscribed, (track) => {
        if (track.kind === Track.Kind.Audio) {
          detachAudio(track);
        }
      })
      .on(RoomEvent.LocalTrackPublished, (publication) => {
        if (publication.kind === Track.Kind.Audio) {
          logEvent('Micrófono activado');
        }
      })
      .on(RoomEvent.MediaDevicesError, (error) => {
        logEvent(`Error del dispositivo de audio: ${error.message}`);
      });

    await room.connect(serverUrlInput.value.trim(), tokenInput.value.trim());
    disconnectButton.disabled = false;
    await room.localParticipant.setMicrophoneEnabled(true);
  } catch (error) {
    if (room) {
      await room.disconnect();
      room = undefined;
    }
    setStatus('No se pudo conectar');
    connectionDetail.textContent = error instanceof Error ? error.message : String(error);
    logEvent(`Error: ${connectionDetail.textContent}`);
    connectButton.disabled = false;
    disconnectButton.disabled = true;
  }
});
