package com.legalia.backend.service;

import com.legalia.backend.model.ActionLog;
import com.legalia.backend.repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActionLogService {

    private final ActionLogRepository actionLogRepository;

    public void log(String email, String action, String details) {
        actionLogRepository.save(new ActionLog(email, action, details));
    }

    public java.util.List<ActionLog> getRecentLogs() {
        return actionLogRepository.findTop50ByOrderByCreatedAtDesc();
    }
}