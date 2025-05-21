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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class QuizController {

    private final DatabaseService db;
    private final QuizUI view;
    private final MainController main;

    private User currentUser;
    private List<Question> allQuestions;
    private List<Question> currentQuestions;
    private List<Question> wrongQuestions;

    public QuizController(DatabaseService db, QuizUI view, MainController main) {
        this.db = db;
        this.view = view;
        this.main = main;
    }

    public void showPlayerSelection() {
        List<String> users = db.loadUsers().stream().map(User::getUsername).collect(Collectors.toList());

        view.showPlayerSelectionPanel(
                this::handleNewPlayer,
                this::handleExistingPlayer,
                this::handleDeletePlayer,
                e -> main.showMainMenu(),
                users
        );
    }

    private void handleNewPlayer(ActionEvent e) {
        String name = JOptionPane.showInputDialog(null, "Name des neuen Spielers:");
        if (name != null && !name.trim().isEmpty()) {
            currentUser = new User(name.trim());
            db.addOrUpdateUser(currentUser);
            showQuizSelection();
        }
    }

    private void handleExistingPlayer(ActionEvent e) {
        String name = e.getActionCommand();
        currentUser = db.loadUsers().stream().filter(u -> u.getUsername().equals(name)).findFirst().orElse(null);
        if (currentUser != null) {
            showQuizSelection();
        }
    }

    private void showQuizSelection() {
        List<String> topics = db.loadTopics();
        view.showQuizSelectionPanel(
                this::startQuiz,
                e -> showPlayerSelection(),
                topics
        );
    }

    private void startQuiz(ActionEvent e) {
        allQuestions = db.loadQuestions();

        String topic = (String) view.topicSelectionCombo.getSelectedItem();
        int difficulty = Integer.parseInt((String) view.difficultySelectionCombo.getSelectedItem());

        currentQuestions = allQuestions.stream()
                .filter(q -> topic.equals(q.getTopic()) && q.getDifficulty() == difficulty)
                .collect(Collectors.toList());

        if (currentQuestions.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Keine passenden Fragen gefunden.");
            showQuizSelection();
            return;
        }

        wrongQuestions = new ArrayList<>();
        askNextQuestion();
    }

    private void askNextQuestion() {
        if (currentQuestions.isEmpty()) {
            if (!wrongQuestions.isEmpty()) {
                currentQuestions.addAll(wrongQuestions);
                wrongQuestions.clear();
                JOptionPane.showMessageDialog(null, "Falsche Fragen werden wiederholt!");
                askNextQuestion();
            } else {
                db.addOrUpdateUser(currentUser);
                JOptionPane.showMessageDialog(null, "Quiz abgeschlossen!");
                main.showMainMenu();
            }
            return;
        }

        Question q = currentQuestions.remove(0);
        String answer = showQuestionDialog(q);
        if (answer != null && answer.equals(q.getAnswers()[q.getCorrectAnswer()])) {
            JOptionPane.showMessageDialog(null, "Richtig!");
        } else {
            JOptionPane.showMessageDialog(null, "Falsch! Richtige Antwort: " + q.getAnswers()[q.getCorrectAnswer()]);
            wrongQuestions.add(q);
            currentUser.addWrongAnswer(q);
        }

        askNextQuestion();
    }

    private String showQuestionDialog(Question question) {
        JDialog dialog = new JDialog((Frame) null, "Quiz", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JLabel label = new JLabel("<html><body style='width:300px'>" + question.getQuestionText() + "</body></html>");
        dialog.add(label, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        final String[] selected = {null};
        for (String ans : question.getAnswers()) {
            JButton btn = new JButton(ans);
            btn.addActionListener(e -> {
                selected[0] = ans;
                dialog.dispose();
            });
            panel.add(btn);
        }

        dialog.add(panel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return selected[0];
    }

    private void handleDeletePlayer(ActionEvent e) {
        String name = view.userList.getSelectedValue();
        if (name != null) {
            int confirm = JOptionPane.showConfirmDialog(null, "Spieler wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                db.deleteUser(name);
                JOptionPane.showMessageDialog(null, "Spieler gelöscht.");
                showPlayerSelection();
            }
        }
    }
}