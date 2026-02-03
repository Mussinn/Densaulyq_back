package com.example.MedSafe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ai-assistant")
@CrossOrigin(origins = "http://localhost:3000")
public class MedicalAIAssistantController {

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // База знаний о симптомах и срочности
    private static final Map<String, SymptomInfo> SYMPTOM_DATABASE = Map.of(
            "головная боль", new SymptomInfo("headache", 2, List.of("невролог", "терапевт")),
            "тошнота", new SymptomInfo("nausea", 2, List.of("терапевт", "гастроэнтеролог")),
            "головокружение", new SymptomInfo("dizziness", 3, List.of("невролог", "кардиолог")),
            "боль в груди", new SymptomInfo("chest_pain", 5, List.of("кардиолог", "скорая")),
            "одышка", new SymptomInfo("shortness_of_breath", 4, List.of("пульмонолог", "терапевт")),
            "высокая температура", new SymptomInfo("high_fever", 3, List.of("инфекционист", "терапевт")),
            "боль в животе", new SymptomInfo("abdominal_pain", 3, List.of("гастроэнтеролог", "хирург"))
    );

    // Красные флаги - симптомы требующие срочной помощи
    private static final List<String> RED_FLAGS = Arrays.asList(
            "сильная боль в груди", "затрудненное дыхание", "потеря сознания",
            "сильное кровотечение", "паралич", "невнятная речь",
            "сильная травма головы", "отравление", "ожог"
    );

    @PostMapping("/analyze-symptoms")
    public ResponseEntity<Map<String, Object>> analyzeSymptoms(
            @RequestBody SymptomRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            String text = request.getSymptoms().toLowerCase();

            // 1. Определяем срочность
            int urgencyLevel = calculateUrgency(text);
            String urgency = getUrgencyDescription(urgencyLevel);

            // 2. Ищем красные флаги
            List<String> detectedRedFlags = detectRedFlags(text);
            boolean needsEmergency = !detectedRedFlags.isEmpty();

            // 3. Определяем возможные диагнозы
            List<String> possibleConditions = analyzeConditions(text);

            // 4. Определяем рекомендованных специалистов
            List<String> recommendedSpecialists = recommendSpecialists(text);

            // 5. Генерируем рекомендации
            List<String> recommendations = generateRecommendations(
                    text, urgencyLevel, needsEmergency, possibleConditions
            );

            // 6. Генерируем уточняющие вопросы (NLP)
            List<String> followUpQuestions = generateFollowUpQuestions(text);

            // 7. Если есть OpenAI API ключ, получаем AI анализ
            Map<String, Object> aiAnalysis = new HashMap<>();
            if (openAiApiKey != null && !openAiApiKey.isEmpty()) {
                aiAnalysis = getOpenAIAnalysis(text);
            }

            response.put("urgencyLevel", urgencyLevel);
            response.put("urgencyDescription", urgency);
            response.put("needsEmergency", needsEmergency);
            response.put("redFlags", detectedRedFlags);
            response.put("possibleConditions", possibleConditions);
            response.put("recommendedSpecialists", recommendedSpecialists);
            response.put("recommendations", recommendations);
            response.put("followUpQuestions", followUpQuestions);
            response.put("aiAnalysis", aiAnalysis);
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Ошибка анализа симптомов: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/emergency-check")
    public ResponseEntity<Map<String, Object>> emergencyCheck(
            @RequestBody SymptomRequest request) {

        Map<String, Object> response = new HashMap<>();
        String text = request.getSymptoms().toLowerCase();

        // Проверка на экстренные случаи
        boolean isEmergency = false;
        String emergencyReason = "";

        for (String flag : RED_FLAGS) {
            if (text.contains(flag)) {
                isEmergency = true;
                emergencyReason = flag;
                break;
            }
        }

        // Дополнительные проверки через регулярные выражения
        if (Pattern.compile("скор(ую|ой)|неотложк|112|103|экстрен").matcher(text).find()) {
            isEmergency = true;
            emergencyReason = "Пациент запрашивает экстренную помощь";
        }

        response.put("isEmergency", isEmergency);
        response.put("emergencyReason", emergencyReason);
        response.put("recommendation", isEmergency ?
                "НЕМЕДЛЕННО ВЫЗВАТЬ СКОРУЮ ПОМОЩЬ ПО ТЕЛЕФОНУ 103" :
                "Экстренная помощь не требуется");
        response.put("emergencyContacts", Arrays.asList(
                "103 - Скорая помощь",
                "112 - Единый номер экстренных служб"
        ));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-questions")
    public ResponseEntity<Map<String, Object>> generateQuestions(
            @RequestBody ConversationRequest request) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> questions = new ArrayList<>();

        // Генерация контекстных вопросов на основе истории диалога
        String lastMessage = request.getMessages().isEmpty() ?
                "" : request.getMessages().get(request.getMessages().size() - 1);

        // Базовые вопросы для уточнения симптомов
        String[] baseQuestions = {
                "Как долго продолжаются симптомы?",
                "Симптомы появились внезапно или постепенно?",
                "Какая интенсивность боли по шкале от 1 до 10?",
                "Есть ли температура? Если да, то какая?",
                "Принимали ли Вы какие-либо лекарства?",
                "Были ли подобные симптомы раньше?",
                "Есть ли хронические заболевания?"
        };

        // Контекстные вопросы на основе симптомов
        if (lastMessage.toLowerCase().contains("голов")) {
            questions.add(createQuestion("Головная боль в какой области? (лоб, виски, затылок)", "location"));
            questions.add(createQuestion("Боль пульсирующая, давящая или острая?", "character"));
            questions.add(createQuestion("Есть ли тошнота или светобоязнь?", "associated"));
        }

        if (lastMessage.toLowerCase().contains("живот") || lastMessage.contains("болит живот")) {
            questions.add(createQuestion("Боль в верхней или нижней части живота?", "location"));
            questions.add(createQuestion("Есть ли тошнота, рвота или диарея?", "digestive"));
            questions.add(createQuestion("Боль постоянная или схваткообразная?", "pattern"));
        }

        // Добавляем базовые вопросы если мало контекстных
        if (questions.size() < 3) {
            for (int i = 0; i < 3 && i < baseQuestions.length; i++) {
                questions.add(createQuestion(baseQuestions[i], "general"));
            }
        }

        response.put("questions", questions);
        response.put("context", "Система задает уточняющие вопросы для точной диагностики");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/self-help/{symptom}")
    public ResponseEntity<Map<String, Object>> getSelfHelpAdvice(
            @PathVariable String symptom) {

        Map<String, Object> advice = new HashMap<>();

        Map<String, List<String>> selfHelpDatabase = Map.of(
                "headache", Arrays.asList(
                        "Отдых в тихом, темном помещении",
                        "Холодный компресс на лоб",
                        "Обильное питье",
                        "Массаж висков",
                        "Избегайте яркого света и громких звуков"
                ),
                "fever", Arrays.asList(
                        "Обильное теплое питье",
                        "Постельный режим",
                        "Прохладные компрессы",
                        "Контроль температуры каждые 4 часа",
                        "Легкая, нежирная пища"
                ),
                "cough", Arrays.asList(
                        "Теплое питье с медом",
                        "Увлажнение воздуха в помещении",
                        "Ингаляции с физраствором",
                        "Отказ от курения",
                        "Полоскание горла солевым раствором"
                )
        );

        String engSymptom = SYMPTOM_DATABASE.containsKey(symptom) ?
                SYMPTOM_DATABASE.get(symptom).getEngName() : symptom;

        List<String> tips = selfHelpDatabase.getOrDefault(engSymptom,
                Arrays.asList("Отдых", "Обильное питье", "Консультация врача при ухудшении"));

        advice.put("symptom", symptom);
        advice.put("selfHelpTips", tips);
        advice.put("whenToSeeDoctor", "Если симптомы сохраняются более 3 дней или ухудшаются");

        return ResponseEntity.ok(advice);
    }

    // Вспомогательные методы
    private int calculateUrgency(String text) {
        int urgency = 1; // По умолчанию низкая

        for (Map.Entry<String, SymptomInfo> entry : SYMPTOM_DATABASE.entrySet()) {
            if (text.contains(entry.getKey())) {
                urgency = Math.max(urgency, entry.getValue().getUrgencyLevel());
            }
        }

        // Повышаем срочность при комбинации симптомов
        String[] words = text.split("\\s+");
        if (words.length > 5) { // Длинное описание часто означает серьезность
            urgency = Math.min(urgency + 1, 5);
        }

        return urgency;
    }

    private String getUrgencyDescription(int level) {
        switch (level) {
            case 1: return "Низкая срочность - плановая консультация";
            case 2: return "Умеренная срочность - консультация в течение 24-48 часов";
            case 3: return "Средняя срочность - консультация сегодня-завтра";
            case 4: return "Высокая срочность - консультация в течение нескольких часов";
            case 5: return "Экстренный случай - немедленная помощь";
            default: return "Неопределенная срочность";
        }
    }

    private List<String> detectRedFlags(String text) {
        List<String> flags = new ArrayList<>();
        for (String flag : RED_FLAGS) {
            if (text.contains(flag)) {
                flags.add(flag);
            }
        }
        return flags;
    }

    private List<String> analyzeConditions(String text) {
        List<String> conditions = new ArrayList<>();

        // Простая логика сопоставления
        if (text.contains("голов") && text.contains("тошнот")) {
            conditions.add("Мигрень");
            conditions.add("Повышенное внутричерепное давление");
        }
        if (text.contains("груд") && text.contains("боль")) {
            conditions.add("Стенокардия");
            conditions.add("Межреберная невралгия");
        }
        if (text.contains("живот") && text.contains("боль")) {
            conditions.add("Гастрит");
            conditions.add("Аппендицит");
        }

        if (conditions.isEmpty()) {
            conditions.add("Требуется дополнительная диагностика");
        }

        return conditions;
    }

    private List<String> recommendSpecialists(String text) {
        Set<String> specialists = new HashSet<>();

        for (Map.Entry<String, SymptomInfo> entry : SYMPTOM_DATABASE.entrySet()) {
            if (text.contains(entry.getKey())) {
                specialists.addAll(entry.getValue().getSpecialists());
            }
        }

        if (specialists.isEmpty()) {
            specialists.add("Терапевт (первичная консультация)");
        }

        return new ArrayList<>(specialists);
    }

    private List<String> generateRecommendations(String text, int urgency,
                                                 boolean emergency, List<String> conditions) {
        List<String> recommendations = new ArrayList<>();

        if (emergency) {
            recommendations.add("🚨 НЕМЕДЛЕННО ВЫЗВАТЬ СКОРУЮ ПОМОЩЬ (103)");
            recommendations.add("Не принимайте пищу и лекарства до осмотра врача");
            recommendations.add("Сохраняйте покой в ожидании помощи");
            return recommendations;
        }

        switch (urgency) {
            case 4:
            case 5:
                recommendations.add("СРОЧНО ОБРАТИТЬСЯ В ПРИЕМНОЕ ОТДЕЛЕНИЕ");
                recommendations.add("Вызвать неотложную помощь");
                break;
            case 3:
                recommendations.add("Записаться на телемедицинскую консультацию сегодня");
                recommendations.add("Ограничить физическую активность");
                break;
            case 2:
                recommendations.add("Записаться на прием к специалисту в ближайшие дни");
                recommendations.add("Наблюдать за динамикой симптомов");
                break;
            case 1:
                recommendations.add("Плановая консультация специалиста");
                recommendations.add("Соблюдать общие рекомендации по здоровью");
                break;
        }

        // Общие рекомендации
        recommendations.add("Вести дневник симптомов с указанием времени и интенсивности");
        recommendations.add("Избегать самолечения без консультации врача");

        return recommendations;
    }

    private List<String> generateFollowUpQuestions(String text) {
        List<String> questions = new ArrayList<>();

        // Генерация вопросов на основе упомянутых симптомов
        if (text.contains("боль")) {
            questions.add("Опишите характер боли (острая, тупая, пульсирующая)?");
            questions.add("Что облегчает боль, а что усиливает?");
        }
        if (text.contains("температур")) {
            questions.add("Какая максимальная температура была?");
            questions.add("Температура постоянная или колеблется в течение дня?");
        }
        if (text.contains("тошнот") || text.contains("рвот")) {
            questions.add("Есть ли связь с приемом пищи?");
            questions.add("Приносит ли рвота облегчение?");
        }

        // Общие вопросы если не найдено специфических
        if (questions.isEmpty()) {
            questions.add("Как давно появились симптомы?");
            questions.add("Были ли подобные проблемы раньше?");
            questions.add("Какие лекарства принимаете регулярно?");
        }

        return questions.subList(0, Math.min(3, questions.size()));
    }

    private Map<String, Object> getOpenAIAnalysis(String text) {
        if (openAiApiKey == null || openAiApiKey.isEmpty()) {
            return Map.of("available", false, "message", "OpenAI API не настроен");
        }

        try {
            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("temperature", 0.7);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                    "Ты медицинский ассистент. Анализируй симптомы пациента на русском языке. " +
                            "Определи возможные причины, срочность и дай рекомендации. " +
                            "Будь осторожен, не ставь диагнозы, а только предполагай возможности."));
            messages.add(Map.of("role", "user", "content",
                    "Симптомы пациента: " + text + "\n\nПроанализируй и дай рекомендации."));

            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            return (Map<String, Object>) response.getBody();

        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "available", false);
        }
    }

    private Map<String, String> createQuestion(String text, String type) {
        Map<String, String> question = new HashMap<>();
        question.put("text", text);
        question.put("type", type);
        question.put("id", UUID.randomUUID().toString());
        return question;
    }

    // DTO классы
    static class SymptomRequest {
        private String symptoms;

        public String getSymptoms() { return symptoms; }
        public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    }

    static class ConversationRequest {
        private List<String> messages;

        public List<String> getMessages() { return messages; }
        public void setMessages(List<String> messages) { this.messages = messages; }
    }

    static class SymptomInfo {
        private String engName;
        private int urgencyLevel;
        private List<String> specialists;

        public SymptomInfo(String engName, int urgencyLevel, List<String> specialists) {
            this.engName = engName;
            this.urgencyLevel = urgencyLevel;
            this.specialists = specialists;
        }

        public String getEngName() { return engName; }
        public int getUrgencyLevel() { return urgencyLevel; }
        public List<String> getSpecialists() { return specialists; }
    }
}
