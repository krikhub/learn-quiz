package l.halbritter.Controller;

import l.halbritter.Database.Database;
import l.halbritter.Model.Question;
import l.halbritter.Model.User;
import l.halbritter.View.QuizUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizController {
    private final QuizUI view;

    private List<Question> allQuestions;
    private List<Question> currentQuestions;
    private List<Question> wrongQuestions;
    private User currentUser;

    public QuizController() {
        view = new QuizUI();
    }

    public void startApplication() {
        Database.connect();
        allQuestions = Database.loadQuestions();

        view.showMainMenu(
                e -> showQuestionsForEditing(),
                e -> showPlayerSelection(),
                e -> showUserSelectionForWorstQuestions(),
                e -> {
                    Database.disconnect();
                    System.exit(0);
                }
        );
    }

    // 1) Auswahl für "10 schlechteste Fragen"
    private void showUserSelectionForWorstQuestions() {
        List<String> existingUsers = Database.loadUsers().stream()
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
        User user = Database.loadUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User nicht gefunden"));

        Map<Integer, Integer> counts = user.getWrongQuestionCounts();
        if (counts.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Keine falsch beantworteten Fragen für ‚" + username + "‘.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
            startApplication();
            return;
        }

        List<Question> questions = Database.loadQuestions();
        List<Map.Entry<Integer, Integer>> topWorst = counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(10)
                .toList();

        StringBuilder sb = new StringBuilder("10 schlechteste Fragen für ‚" + username + "‘:\n\n");
        for (var entry : topWorst) {
            int qId = entry.getKey();
            int wrongCount = entry.getValue();
            questions.stream()
                    .filter(q -> q.getQuestionId() == qId)
                    .findFirst()
                    .ifPresent(q -> sb.append("- ")
                            .append(q.getQuestionText())
                            .append("  (falsch: ")
                            .append(wrongCount)
                            .append("x)\n"));
        }

        JOptionPane.showMessageDialog(
                null,
                sb.toString(),
                "Schlechteste Fragen",
                JOptionPane.INFORMATION_MESSAGE
        );
        startApplication();
    }

    // 2) Spieler-Auswahl vor Quiz
    private void showPlayerSelection() {
        List<String> existingUsers = Database.loadUsers().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        view.showPlayerSelectionPanel(
                this::handleNewPlayer,
                this::handleExistingPlayer,
                this::handleDeletePlayer,     // neu!
                this::handleCancelToMain,
                existingUsers
        );
    }

    private void handleNewPlayer(ActionEvent e) {
        String name = JOptionPane.showInputDialog(null, "Bitte Namen des neuen Spielers eingeben:");
        if (name != null && !name.trim().isEmpty()) {
            currentUser = new User(name.trim());
            Database.addOrUpdateUser(currentUser);
            showQuizSelection();
        }
    }

    private void handleExistingPlayer(ActionEvent e) {
        String selectedName = e.getActionCommand();
        currentUser = Database.loadUsers().stream()
                .filter(u -> u.getUsername().equals(selectedName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User nicht gefunden"));
        showQuizSelection();
    }

    // 3) Quiz-Auswahl
    private void showQuizSelection() {
        List<String> topics = Database.loadTopics();
        view.showQuizSelectionPanel(
                this::handleBeginQuiz,
                this::handleCancelToPlayerSelection,
                topics
        );
    }

    private void handleCancelToPlayerSelection(ActionEvent e) {
        showPlayerSelection();
    }

    // 4) Quiz starten
    private void handleBeginQuiz(ActionEvent e) {
        // 1) Thema und Schwierigkeit aus der UI auslesen
        String topic = (String) view.topicSelectionCombo.getSelectedItem();
        String diffStr = (String) view.difficultySelectionCombo.getSelectedItem();
        int difficulty = Integer.parseInt(diffStr);

        // 2) Fragen filtern nach Thema UND Schwierigkeit
        currentQuestions = allQuestions.stream()
                .filter(q -> topic.equals(q.getTopic()) && q.getDifficulty() == difficulty)
                .collect(Collectors.toList());

        // 3) OPTIONALE LÖSUNG: Abfangen, wenn gar keine Fragen übrigbleiben
        if (currentQuestions.isEmpty()) {
            // Hinweis-Dialog
            JOptionPane.showMessageDialog(
                    null,
                    "Keine Fragen für Thema „" + topic + "“ …",
                    "Keine Fragen",
                    JOptionPane.WARNING_MESSAGE
            );
            // Quiz-Auswahl wieder anzeigen (wenn du eine eigene Methode dafür hast)
            showQuizSelection();
            return;  // Methode hier beenden, Quiz startet nicht
        }

        // 4) Sonst Quiz normal starten
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
            Database.deleteUser(name);
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
                Database.addOrUpdateUser(currentUser);
                JOptionPane.showMessageDialog(null, "Quiz abgeschlossen!");
                startApplication();
            }
            return;
        }

        Question question = currentQuestions.remove(0);
        String userAnswer = (String) JOptionPane.showInputDialog(
                null,
                question.getQuestionText(),
                "Quiz",
                JOptionPane.QUESTION_MESSAGE,
                null,
                question.getAnswers(),
                question.getAnswers()[0]
        );

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

    // 5) Fragen bearbeiten
    private void showQuestionsForEditing() {
        allQuestions = Database.loadQuestions();
        Map<String, List<Question>> questionsByTopic = allQuestions.stream()
                .filter(q -> q.getTopic() != null)
                .collect(Collectors.groupingBy(Question::getTopic));

        view.addTopicButton.addActionListener(evt -> {
            String newTopic = JOptionPane.showInputDialog(
                    view.getFrame(),
                    "Neues Thema eingeben:"
            );
            if (newTopic == null) {
                return; // Abbruch
            }
            newTopic = newTopic.trim();
            if (newTopic.isEmpty()) {
                JOptionPane.showMessageDialog(
                        view.getFrame(),
                        "Das Themenfeld darf nicht leer sein.",
                        "Ungültige Eingabe",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            Database.addTopic(newTopic);
            JOptionPane.showMessageDialog(
                    view.getFrame(),
                    "Thema hinzugefügt!",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE
            );
            showQuestionsForEditing();
        });

        // Thema löschen
        view.deleteTopicButton.addActionListener(evt -> {
            List<String> topics = Database.loadTopics();
            if (topics.isEmpty()) {
                JOptionPane.showMessageDialog(
                        view.getFrame(),
                        "Keine Themen zum Löschen vorhanden!",
                        "Keine Themen",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            String[] options = topics.toArray(new String[0]);
            String topicToDelete = (String) JOptionPane.showInputDialog(
                    view.getFrame(),
                    "Thema zum Löschen auswählen:",
                    "Thema löschen",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (topicToDelete != null) {
                int confirm = JOptionPane.showConfirmDialog(
                        view.getFrame(),
                        "Möchtest du das Thema \"" + topicToDelete + "\" wirklich löschen?",
                        "Thema löschen",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm != JOptionPane.YES_OPTION) return;
                try {
                    Database.deleteTopic(topicToDelete);
                    JOptionPane.showMessageDialog(
                            view.getFrame(),
                            "Thema „" + topicToDelete + "“ gelöscht!",
                            "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    showQuestionsForEditing();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            view.getFrame(),
                            "Fehler beim Löschen des Themas: " + ex.getMessage(),
                            "Datenbankfehler",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // Entscheide, ob man Fragen anlegen darf
        List<String> topics = Database.loadTopics();
        ActionListener createListener = topics.isEmpty()
                ? null
                : e -> view.showEditQuizPanel(
                this::handleCreateQuestion,
                null,
                this::handleCancelToQuestions,
                topics
        );

        view.showQuestionListPanel(
                questionsByTopic,
                this::handleEditQuestion,
                createListener,
                this::handleCancelToMain
        );
    }

    private void handleEditQuestion(ActionEvent e) {
        int questionId = Integer.parseInt(e.getActionCommand());
        Question questionToEdit = allQuestions.stream()
                .filter(q -> q.getQuestionId() == questionId)
                .findFirst()
                .orElse(null);

        if (questionToEdit != null) {
            List<String> topics = Database.loadTopics();
            view.showEditQuizPanel(
                    evt -> createOrUpdateQuestion(questionToEdit),
                    evt -> handleDeleteQuestion(questionToEdit),
                    this::handleCancelToQuestions,
                    topics
            );
            // Felder vorbelegen
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
        // Validierung: mindestens ein Thema vorhanden
        if (view.topicsCombo.getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                    view.getFrame(),
                    "Bitte lege zuerst mindestens ein Thema an.",
                    "Kein Thema vorhanden",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        String topic = (String) view.topicsCombo.getSelectedItem();
        if (topic == null || topic.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    view.getFrame(),
                    "Bitte ein Thema auswählen.",
                    "Ungültige Eingabe",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String questionText = view.questionField.getText().trim();
        if (questionText.isEmpty()) {
            JOptionPane.showMessageDialog(
                    view.getFrame(),
                    "Der Fragetext darf nicht leer sein.",
                    "Ungültige Eingabe",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String[] answers = new String[4];
        for (int i = 0; i < 4; i++) {
            answers[i] = view.answerFields[i].getText().trim();
            if (answers[i].isEmpty()) {
                JOptionPane.showMessageDialog(
                        view.getFrame(),
                        "Antwort " + (i + 1) + " darf nicht leer sein.",
                        "Ungültige Eingabe",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        int correctAnswer = view.correctAnswerCombo.getSelectedIndex();
        if (correctAnswer < 0 || correctAnswer > 3) {
            JOptionPane.showMessageDialog(
                    view.getFrame(),
                    "Bitte eine gültige richtige Antwort auswählen.",
                    "Ungültige Eingabe",
                    JOptionPane.WARNING_MESSAGE
            );
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
            Database.addOrUpdateQuestion(question);
            JOptionPane.showMessageDialog(
                    view.getFrame(),
                    "Frage wurde gespeichert!",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE
            );
            startApplication();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    view.getFrame(),
                    "Fehler beim Speichern: " + ex.getMessage(),
                    "Datenbankfehler",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
    private void handleDeleteQuestion(Question questionToDelete) {
        int ans = JOptionPane.showConfirmDialog(
                view.getFrame(),
                "Soll die Frage wirklich gelöscht werden?\n\"" + questionToDelete.getQuestionText() + "\"",
                "Frage löschen",
                JOptionPane.YES_NO_OPTION
        );
        if (ans == JOptionPane.YES_OPTION) {
            try {
                Database.deleteQuestion(questionToDelete.getQuestionId());
                JOptionPane.showMessageDialog(
                        view.getFrame(),
                        "Frage wurde gelöscht!",
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE
                );
                showQuestionsForEditing();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        view.getFrame(),
                        "Fehler beim Löschen der Frage: " + ex.getMessage(),
                        "Datenbankfehler",
                        JOptionPane.ERROR_MESSAGE
                );
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