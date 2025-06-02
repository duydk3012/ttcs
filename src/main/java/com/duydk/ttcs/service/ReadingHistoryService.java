package com.duydk.ttcs.service;

import com.duydk.ttcs.repository.ReadingHistoryRepository;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Data
@Service
@Transactional
public class ReadingHistoryService {
    private final ReadingHistoryRepository readingHistoryRepository;

    public ReadingHistoryService(ReadingHistoryRepository readingHistoryRepository) {
        this.readingHistoryRepository = readingHistoryRepository;
    }
}
