package com.duydk.ttcs.controller;

import com.duydk.ttcs.entity.Voice;
import com.duydk.ttcs.service.VoiceService;
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/speech")
public class SpeechController {

    private static final Logger logger = LoggerFactory.getLogger(SpeechController.class);
    private final VoiceService voiceService;

    public SpeechController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    // Hiển thị trang nhập văn bản
    @GetMapping
    public String showSpeechPage(Model model) {
        List<Voice> voices = voiceService.getVoiceRepository().findAll();
        model.addAttribute("voices", voices);
        model.addAttribute("text", "");
        model.addAttribute("selectedVoice", "");
        return "speech";
    }

    // Xử lý yêu cầu chuyển văn bản thành giọng nói và phát trực tiếp
    @PostMapping("/convert")
    @ResponseBody
    public ResponseEntity<String> convertTextToSpeech(@RequestParam String text, @RequestParam String voiceName) {
        try {
            // Kiểm tra đầu vào
            if (text == null || text.trim().isEmpty()) {
                logger.error("Văn bản trống");
                return ResponseEntity.badRequest().body("Văn bản không được để trống");
            }
            if (!voiceService.getVoiceRepository().existsByName(voiceName)) {
                logger.error("Giọng nói không hợp lệ: {}", voiceName);
                return ResponseEntity.badRequest().body("Giọng nói không hợp lệ: " + voiceName);
            }

            // Kiểm tra credentials
            String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (credentialsPath == null) {
                logger.error("Biến môi trường GOOGLE_APPLICATION_CREDENTIALS chưa được thiết lập");
                return ResponseEntity.status(500).body("Lỗi: Chưa thiết lập credentials");
            }
            logger.info("Credentials path: {}", credentialsPath);

            try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
                logger.info("TextToSpeechClient khởi tạo thành công");

                // Tạo input text
                SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

                // Cấu hình giọng nói
                VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode("vi-VN")
                        .setSsmlGender(SsmlVoiceGender.MALE) // Giữ giống TextToSpeechExample
                        .setName(voiceName)
                        .build();

                // Cấu hình audio
                AudioConfig audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .setSpeakingRate(1.0)
                        .setPitch(0.0)
                        .build();

                // Thực hiện chuyển đổi
                SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
                ByteString audioContents = response.getAudioContent();

                // Trả về base64
                String base64Audio = Base64.getEncoder().encodeToString(audioContents.toByteArray());
                return ResponseEntity.ok(base64Audio);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển đổi văn bản thành giọng nói: ", e);
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    // Xử lý yêu cầu tải file MP3
    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadMp3(@RequestParam String text, @RequestParam String voiceName) {
        try {
            // Kiểm tra đầu vào
            if (text == null || text.trim().isEmpty()) {
                logger.error("Văn bản trống");
                return ResponseEntity.badRequest().build();
            }
            if (!voiceService.getVoiceRepository().existsByName(voiceName)) {
                logger.error("Giọng nói không hợp lệ: {}", voiceName);
                return ResponseEntity.badRequest().build();
            }

            try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
                // Tạo input text
                SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

                // Cấu hình giọng nói
                VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode("vi-VN")
                        .setSsmlGender(SsmlVoiceGender.FEMALE)
                        .setName(voiceName)
                        .build();

                // Cấu hình audio
                AudioConfig audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .setSpeakingRate(1.0)
                        .setPitch(0.0)
                        .build();

                // Thực hiện chuyển đổi
                SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
                byte[] audioBytes = response.getAudioContent().toByteArray();

                // Thiết lập header
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", "output.mp3");
                headers.setContentLength(audioBytes.length);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(audioBytes);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi tải file MP3: ", e);
            return ResponseEntity.status(500).build();
        }
    }
}