<template>
  <div>
    <div class="audioWrapper">
      <h3>recording test</h3>
      <button @click="startRecording" :disabled="isRecording">START</button>
      <button @click="stopRecording">STOP</button>
      <div v-if="isRecording" class="recordingText">RECORDING</div>
    </div>
    <div class="audioWrapper">
      <audio controls>
        <source id="audioPreview" src="" type="audio/mp3" />
      </audio>
    </div>

    <div class="audioWrapper">
      <h3>PLAYBACK</h3>
      <button @click="startPlaying" :disabled="isRecording">START</button>
      <button @click="stopPlaying">STOP</button>
    </div>
  </div>
</template>

<script >
import { ref } from "vue";
import { Plugins, Capacitor } from "@capacitor/core";

export default {
  name: "AudioRecorder",
  props: {
    msg: String
  },
  setup() {
    const AudioRecorder = Plugins.AudioRecorder || Plugins.AudioRecorderPlugin;
    const isRecording = ref(false);
    let filePath = "";

    const listener = AudioRecorder.addListener(
      "recordingFinished",
      async info => {
        console.log("recordingFinished was fired");

        isRecording.value = false;
        console.log("file", Capacitor.convertFileSrc(info.file));
        filePath = info.file;
        console.log("duration", info.duration);
        document.getElementById("audioPreview").src = Capacitor.convertFileSrc(
          info.file
        );

        console.log("Converting to blob...");
        const blob = await fetch(Capacitor.convertFileSrc(info.file)).then(r =>
          r.blob()
        );
        console.log("As BLOB", blob);
      }
    );

    AudioRecorder.authorize();

    const startRecording = async () => {
      const r = await AudioRecorder.start({
        fileName: "test.m4a",
        duration: 6.0
      });
      isRecording.value = true;
      console.log(r);
    };

    const stopRecording = async () => {
      isRecording.value = false;
      const r = await AudioRecorder.stop();
    };

    const startPlaying = async () => {
      await AudioRecorder.startPlaying({ fileName: filePath });
    };

    const stopPlaying = async () => {
      await AudioRecorder.stopPlaying();

      await AudioRecorder.deleteRecording({ fileName: filePath });
    };

    return {
      startRecording,
      stopRecording,
      startPlaying,
      stopPlaying,
      isRecording
    };
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.recordingText {
  font-weight: bold;
  color: red;
  padding: 16px;
}
.audioWrapper {
  padding: 16px;
}
.hide-audio {
  display: none;
}
</style>
