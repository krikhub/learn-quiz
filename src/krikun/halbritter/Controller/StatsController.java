/*
 * © 2025 Luca Halbritter und Alexander Krikun
 * IU Internationale Hochschule, Hamburg
 * SPDX-License-Identifier: MIT
 */
package krikun.halbritter.Controller;

import krikun.halbritter.Database.DatabaseService;
import krikun.halbritter.Model.Question;
import krikun.halbritter.Model.User;
import krikun.halbritter.View.QuizUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.stream.Collectors;

public class StatsController {

    private final DatabaseService db;
    private final QuizUI view;
    private final MainController main;

    public StatsController(DatabaseService db, QuizUI view, MainController main) {
        this.db = db;
        this.view = view;
        this.main = main;
    }

    public void showUserSelectionForWorstQuestions() {
        List<String> users = db.loadUsers().stream().map(User::getUsername).collect(Collectors.toList());

        view.showUserSelectionPanel(
                this::handleUserSelected,
                e -> main.showMainMenu(),
                users
        );
    }

    private void handleUserSelected(ActionEvent e) {
        String username = e.getActionCommand();
        User user = db.loadUsers().stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
        if (user == null || user.getWrongQuestionCounts().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Keine Statistikdaten vorhanden.");
            return;
        }

        List<String> topics = db.loadTopics();
        String[] options = new String[topics.size() + 1];
        options[0] = "Overall";
        for (int i = 0; i < topics.size(); i++) options[i + 1] = topics.get(i);

        String choice = (String) JOptionPane.showInputDialog(null, "Thema wählen:", "Top 10", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == null) return;

        List<Question> allQuestions = db.loadQuestions();
        var stream = user.getWrongQuestionCounts().entrySet().stream();

        if (!"Overall".equals(choice)) {
            stream = stream.filter(e1 ->
                    allQuestions.stream().anyMatch(q -> q.getQuestionId() == e1.getKey() && choice.equals(q.getTopic())));
        }

        var topWorst = stream.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(10).toList();

        StringBuilder sb = new StringBuilder("Top 10 Fragen für „").append(username).append("“:\n\n");
        for (var entry : topWorst) {
            allQuestions.stream()
                    .filter(q -> q.getQuestionId() == entry.getKey())
                    .findFirst()
                    .ifPresent(q -> sb.append("- ").append(q.getQuestionText()).append(" (").append(q.getTopic()).append("): ").append(entry.getValue()).append("× falsch\n"));
        }

        JOptionPane.showMessageDialog(null, sb.toString(), "Statistik", JOptionPane.INFORMATION_MESSAGE);
    }
}