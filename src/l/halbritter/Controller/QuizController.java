package l.halbritter.Controller;

import l.halbritter.Database.DatabaseService;
import l.halbritter.Model.Question;
import l.halbritter.Model.User;
import l.halbritter.View.QuizUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizController {

    private final QuizUI view;
    private final DatabaseService db;

    private List<Question> allQuestions;
    private List<Question> currentQuestions;
    private List<Question> wrongQuestions;
    private User currentUser;

    public QuizController(DatabaseService dbService) {
        this.view = new QuizUI();
        this.db = dbService;
    }

    public void startApplication() {
        db.connect();
        allQuestions = db.loadQuestions();

        view.showMainMenu(
                e -> showQuestionsForEditing(),
                e -> showPlayerSelection(),
                e -> showUserSelectionForWorstQuestions(),
                e -> {
                    db.disconnect();
                    System.exit(0);
                }
        );
    }

    private void showUserSelectionForWorstQuestions() {
        List<String> existingUsers = db.loadUsers().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        view.showUserSelectionPanel(
                this::handleUserSelectedForWorstQuestions,
                this::handleCancelToMain,
                existingUsers
        );
    }

    private void handleUserSelectedForWorstQuestions(ActionEvent e) {
        String username = e.getActionCommand();
        User user = db.loadUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User nicht gefunden"));

        Map<Integer, Integer> counts = user.getWrongQuestionCounts();
        if (counts.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Keine falsch beantworteten Fragen für „" + username + "“.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
            startApplication();
            return;
        }

        List<String> topics = db.loadTopics();
        String[] options = new String[topics.size() + 1];
        options[0] = "Overall";
        for (int i = 0; i < topics.size(); i++) {
            options[i + 1] = topics.get(i);
        }
        String choice = (String) JOptionPane.showInputDialog(
                null,
                "Welche Liste möchtest du sehen?",
                "Schlechteste Fragen für „" + username + "“",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice == null) {
            startApplication();
            return;
        }

        List<Question> allQuestions = db.loadQuestions();
        var stream = counts.entrySet().stream();
        if (!choice.equals("Overall")) {
            stream = stream.filter(entry -> {
                int qId = entry.getKey();
                return allQuestions.stream()
                        .anyMatch(q -> q.getQuestionId() == qId && choice.equals(q.getTopic()));
            });
        }

        List<Map.Entry<Integer, Integer>> topWorst = stream
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        if (choice.equals("Overall")) {
            sb.append("Top 10 schlechteste Fragen (Overall) für „").append(username).append("“:\n\n");
        } else {
            sb.append("Top 10 schlechteste Fragen zum Thema „").append(choice)
                    .append("“ für „").append(username).append("“:\n\n");
        }
        for (var entry : topWorst) {
            int qId = entry.getKey();
            int wrongCount = entry.getValue();
            allQuestions.stream()
                    .filter(q -> q.getQuestionId() == qId)
                    .findFirst()
                    .ifPresent(q -> sb.append("- ")
                            .append(q.getQuestionText())
                            .append("  [Thema: ").append(q.getTopic()).append("] ")
                            .append("(falsch: ").append(wrongCount).append("×)\n"));
        }

        JOptionPane.showMessageDialog(
                null,
                sb.toString(),
                "Schlechteste Fragen",
                JOptionPane.INFORMATION_MESSAGE
        );
        startApplication();
    }

    private void showPlayerSelection() {
        List<String> existingUsers = db.loadUsers().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        view.showPlayerSelectionPanel(
                this::handleNewPlayer,
                this::handleExistingPlayer,
                this::handleDeletePlayer,
                this::handleCancelToMain,
                existingUsers
        );
    }

    private void handleNewPlayer(ActionEvent e) {
        String name = JOptionPane.showInputDialog(null, "Bitte Namen des neuen Spielers eingeben:");
        if (name != null && !name.trim().isEmpty()) {
            currentUser = new User(name.trim());
            db.addOrUpdateUser(currentUser);
            showQuizSelection();
        }
    }

    private void handleExistingPlayer(ActionEvent e) {
        String selectedName = e.getActionCommand();
        currentUser = db.loadUsers().stream()
                .filter(u -> u.getUsername().equals(selectedName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User nicht gefunden"));
        showQuizSelection();
    }

    private void showQuizSelection() {
        List<String> topics = db.loadTopics();
        view.showQuizSelectionPanel(
                this::handleBeginQuiz,
                this::handleCancelToPlayerSelection,
                topics
        );
    }

    private void handleCancelToPlayerSelection(ActionEvent e) {
        showPlayerSelection();
    }

    private void handleBeginQuiz(ActionEvent e) {
        String topic = (String) view.topicSelectionCombo.getSelectedItem();
        String diffStr = (String) view.difficultySelectionCombo.getSelectedItem();
        int difficulty = Integer.parseInt(diffStr);

        currentQuestions = allQuestions.stream()
                .filter(q -> topic.equals(q.getTopic()) && q.getDifficulty() == difficulty)
                .collect(Collectors.toList());

        if (currentQuestions.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Keine Fragen für Thema „" + topic + "“ …",
                    "Keine Fragen",
                    JOptionPane.WARNING_MESSAGE
            );
            showQuizSelection();
            return;
        }

        wrongQuestions = new ArrayList<>();
        askNextQuestion();
    }

    private void handleDeletePlayer(ActionEvent e) {
        String name = view.userList.getSelectedValue();
        if (name == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "Bitte zuerst einen Spieler aus der Liste auswählen.",
                    "Kein Spieler ausgewählt",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        int ans = JOptionPane.showConfirmDialog(
                null,
                "Soll Spieler \"" + name + "\" wirklich gelöscht werden?\nAlle statistischen Daten gehen verloren.",
                "Spieler löschen",
                JOptionPane.YES_NO_OPTION
        );
        if (ans == JOptionPane.YES_OPTION) {
            db.deleteUser(name);
            JOptionPane.showMessageDialog(null, "Spieler \"" + name + "\" wurde gelöscht.");
            showPlayerSelection();
        }
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
                startApplication();
            }
            return;
        }

        Question question = currentQuestions.remove(0);
        String userAnswer = showQuestionDialog(question);

        if (userAnswer != null && userAnswer.equals(question.getAnswers()[question.getCorrectAnswer()])) {
            JOptionPane.showMessageDialog(null, "Richtig!");
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "Falsch! Richtige Antwort: " + question.getAnswers()[question.getCorrectAnswer()]
            );
            wrongQuestions.add(question);
            currentUser.addWrongAnswer(question);
        }
        askNextQuestion();
    }

    private String showQuestionDialog(Question question) {
        JDialog dialog = new JDialog((Frame) null, "Quiz", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JLabel lblQuestion = new JLabel("<html><body style='width:300px'>" + question.getQuestionText() + "</body></html>");
        lblQuestion.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        dialog.add(lblQuestion, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        String[] answers = question.getAnswers();
        final String[] selected = { null };

        for (String ans : answers) {
            JButton btn = new JButton(ans);
            btn.addActionListener(e -> {
                selected[0] = ans;
                dialog.dispose();
            });
            buttonPanel.add(btn);
        }
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        dialog.add(buttonPanel, BorderLayout.CENTER);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return selected[0];
    }

    private void showQuestionsForEditing() {
        allQuestions = db.loadQuestions();
        Map<String, List<Question>> questionsByTopic = allQuestions.stream()
                .filter(q -> q.getTopic() != null)
                .collect(Collectors.groupingBy(Question::getTopic));

        view.addTopicButton.addActionListener(evt -> {
            String newTopic = JOptionPane.showInputDialog(view.getFrame(), "Neues Thema eingeben:");
            if (newTopic == null) return;
            newTopic = newTopic.trim();
            if (newTopic.isEmpty()) {
                JOptionPane.showMessageDialog(view.getFrame(), "Das Themenfeld darf nicht leer sein.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
                return;
            }
            db.addTopic(newTopic);
            JOptionPane.showMessageDialog(view.getFrame(), "Thema hinzugefügt!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        });

        view.deleteTopicButton.addActionListener(evt -> {
            List<String> topics = db.loadTopics();
            if (topics.isEmpty()) {
                JOptionPane.showMessageDialog(view.getFrame(), "Keine Themen zum Löschen vorhanden!", "Keine Themen", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] options = topics.toArray(new String[0]);
            String topicToDelete = (String) JOptionPane.showInputDialog(view.getFrame(), "Thema zum Löschen auswählen:", "Thema löschen", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (topicToDelete != null) {
                int confirm = JOptionPane.showConfirmDialog(view.getFrame(), "Möchtest du das Thema \"" + topicToDelete + "\" wirklich löschen?", "Thema löschen", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                try {
                    db.deleteTopic(topicToDelete);
                    JOptionPane.showMessageDialog(view.getFrame(), "Thema „" + topicToDelete + "“ gelöscht!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view.getFrame(), "Fehler beim Löschen des Themas: " + ex.getMessage(), "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        List<String> topics = db.loadTopics();
        ActionListener createListener = topics.isEmpty()
                ? null
                : e -> view.showEditQuizPanel(this::handleCreateQuestion, null, this::handleCancelToQuestions, topics);

        view.showQuestionListPanel(questionsByTopic, this::handleEditQuestion, createListener, this::handleCancelToMain);
    }

    private void handleEditQuestion(ActionEvent e) {
        int questionId = Integer.parseInt(e.getActionCommand());
        Question questionToEdit = allQuestions.stream()
                .filter(q -> q.getQuestionId() == questionId)
                .findFirst()
                .orElse(null);

        if (questionToEdit != null) {
            List<String> topics = db.loadTopics();
            view.showEditQuizPanel(
                    evt -> createOrUpdateQuestion(questionToEdit),
                    evt -> handleDeleteQuestion(questionToEdit),
                    this::handleCancelToQuestions,
                    topics
            );
            view.topicsCombo.setSelectedItem(questionToEdit.getTopic());
            view.questionField.setText(questionToEdit.getQuestionText());
            String[] answers = questionToEdit.getAnswers();
            for (int i = 0; i < 4; i++) {
                view.answerFields[i].setText(answers[i]);
            }
            view.correctAnswerCombo.setSelectedIndex(questionToEdit.getCorrectAnswer());
            view.difficultyCombo.setSelectedItem(String.valueOf(questionToEdit.getDifficulty()));
        }
    }

    private void handleCreateQuestion(ActionEvent e) {
        createOrUpdateQuestion(null);
    }

    private void createOrUpdateQuestion(Question existingQuestion) {
        if (view.topicsCombo.getItemCount() == 0) {
            JOptionPane.showMessageDialog(view.getFrame(), "Bitte lege zuerst mindestens ein Thema an.", "Kein Thema vorhanden", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String topic = (String) view.topicsCombo.getSelectedItem();
        if (topic == null || topic.trim().isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(), "Bitte ein Thema auswählen.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String questionText = view.questionField.getText().trim();
        if (questionText.isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(), "Der Fragetext darf nicht leer sein.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] answers = new String[4];
        for (int i = 0; i < 4; i++) {
            answers[i] = view.answerFields[i].getText().trim();
            if (answers[i].isEmpty()) {
                JOptionPane.showMessageDialog(view.getFrame(), "Antwort " + (i + 1) + " darf nicht leer sein.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int correctAnswer = view.correctAnswerCombo.getSelectedIndex();
        if (correctAnswer < 0 || correctAnswer > 3) {
            JOptionPane.showMessageDialog(view.getFrame(), "Bitte eine gültige richtige Antwort auswählen.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int difficulty = Integer.parseInt((String) view.difficultyCombo.getSelectedItem());

        Question question = existingQuestion != null ? existingQuestion : new Question();
        if (existingQuestion == null) {
            question.setQuestionId(new Random().nextInt(10_000));
        }
        question.setTopic(topic);
        question.setQuestionText(questionText);
        question.setAnswers(answers);
        question.setCorrectAnswer(correctAnswer);
        question.setDifficulty(difficulty);

        try {
            db.addOrUpdateQuestion(question);
            JOptionPane.showMessageDialog(view.getFrame(), "Frage wurde gespeichert!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            startApplication();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view.getFrame(), "Fehler beim Speichern: " + ex.getMessage(), "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteQuestion(Question questionToDelete) {
        int ans = JOptionPane.showConfirmDialog(view.getFrame(), "Soll die Frage wirklich gelöscht werden?\n\"" + questionToDelete.getQuestionText() + "\"", "Frage löschen", JOptionPane.YES_NO_OPTION);
        if (ans == JOptionPane.YES_OPTION) {
            try {
                db.deleteQuestion(questionToDelete.getQuestionId());
                JOptionPane.showMessageDialog(view.getFrame(), "Frage wurde gelöscht!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                showQuestionsForEditing();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(view.getFrame(), "Fehler beim Löschen der Frage: " + ex.getMessage(), "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCancelToQuestions(ActionEvent e) {
        showQuestionsForEditing();
    }

    private void handleCancelToMain(ActionEvent e) {
        startApplication();
    }
}