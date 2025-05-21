/*
 * © 2025 Luca Halbritter und Alexander Krikun
 * IU Internationale Hochschule, Hamburg
 * SPDX-License-Identifier: MIT
 */
package krikun.halbritter.Model;

import java.util.*;
import java.util.stream.Collectors;

public class User {
    private String username;
    private Map<Integer, Integer> wrongQuestionCounts;

    public User() {
        this.username = "";
        this.wrongQuestionCounts = new HashMap<>();
    }

    public User(String username) {
        this.username = username;
        this.wrongQuestionCounts = new HashMap<>();
    }

    public String getUsername() {
        return username;
    }

    public Map<Integer, Integer> getWrongQuestionCounts() {
        return wrongQuestionCounts;
    }

    public void setWrongQuestionCounts(Map<Integer, Integer> wrongCounts) {
        this.wrongQuestionCounts = wrongCounts;
    }

    public void addWrongAnswer(Question question) {
        int questionId = question.getQuestionId();
        wrongQuestionCounts.put(questionId, wrongQuestionCounts.getOrDefault(questionId, 0) + 1);
    }

    public void clearWrongAnswers() {
        wrongQuestionCounts.clear();
    }

    public List<Question> getTopWorstQuestions(List<Question> allQuestions) {
        return wrongQuestionCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .map(entry -> allQuestions.stream()
                        .filter(q -> q.getQuestionId() == entry.getKey())
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
