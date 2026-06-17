package com.example.Cricbook.service;

import com.example.Cricbook.entity.NotificationTemplate;
import com.example.Cricbook.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;

    public String render(String body, Map<String, String> variables) {
        if (body == null) return "";
        if (variables == null || variables.isEmpty()) return body;

        String rendered = body;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace(placeholder, value);
        }
        return rendered;
    }

    public String renderTemplate(String templateName, Map<String, String> variables) {
        Optional<NotificationTemplate> optionalTemplate = templateRepository.findByName(templateName);
        if (optionalTemplate.isEmpty()) {
            log.warn("Template not found: {}, rendering fallback body", templateName);
            // Fallback rendering based on map keys if template doesn't exist yet
            return variables.toString();
        }
        return render(optionalTemplate.get().getBody(), variables);
    }
}
