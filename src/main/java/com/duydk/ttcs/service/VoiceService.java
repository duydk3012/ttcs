package com.duydk.ttcs.service;

import com.duydk.ttcs.repository.VoiceRepository;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Data
@Service
@Transactional
public class VoiceService {
    private final VoiceRepository voiceRepository;

    public VoiceService(VoiceRepository voiceRepository) {
        this.voiceRepository = voiceRepository;
    }
}
