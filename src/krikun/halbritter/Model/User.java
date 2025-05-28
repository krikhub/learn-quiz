/*
 * © 2025 Luca Halbritter und Alexander Krikun
 * IU Internationale Hochschule, Hamburg
 * SPDX-License-Identifier: MIT
 */
package krikun.halbritter.Model;

import java.util.*;

public class User {
    private String username;
    private Map<Integer, Integer> wrongQuestionCounts;

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

    public void addWrongAnswer(Question question) {
        int questionId = question.getQuestionId();
        wrongQuestionCounts.put(questionId, wrongQuestionCounts.getOrDefault(questionId, 0) + 1);
    }

}
