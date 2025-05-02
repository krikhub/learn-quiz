package l.halbritter.Controller;

import l.halbritter.Database.Database;
import l.halbritter.Model.Question;
import l.halbritter.Model.User;
import l.halbritter.View.QuizUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
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
                e -> {
                    List<String> existingUsers = Database.loadUsers().stream()
                            .map(User::getUsername)
                            .collect(Collectors.toList());
                    List<String> topics = allQuestions.stream()
                            .map(Question::getTopic)
                            .distinct()
                            .collect(Collectors.toList());
                    view.showStartQuizPanel(this::handleStartQuiz, this::handleCancel, existingUsers, topics);
                },
                e -> {
                    Database.disconnect();
                    System.exit(0);
                }
        );
    }

    private void showQuestionsForEditing() {
        allQuestions = Database.loadQuestions();
        Map<String, List<Question>> questionsByTopic = allQuestions.stream()
                .filter(q -> q.getTopic() != null)
                .collect(Collectors.groupingBy(Question::getTopic));

        view.addTopicButton.addActionListener(evt -> {
            String newTopic = JOptionPane.showInputDialog(null, "Neues Thema eingeben:");
            if (newTopic != null && !newTopic.trim().isEmpty()) {
                Database.addTopic(newTopic.trim());
                JOptionPane.showMessageDialog(null, "Thema hinzugefügt!");
                showQuestionsForEditing();
            }
        });

        view.deleteTopicButton.addActionListener(evt -> {
            String topicToDelete = JOptionPane.showInputDialog(null, "Thema zum Löschen eingeben:");
            if (topicToDelete != null && !topicToDelete.trim().isEmpty()) {
                Database.deleteTopic(topicToDelete.trim());
                JOptionPane.showMessageDialog(null, "Thema gelöscht!");
                showQuestionsForEditing();
            }
        });

        view.showQuestionListPanel(
                questionsByTopic,
                this::handleEditQuestion,
                e -> {
                    List<String> topics = Database.loadTopics();
                    view.showEditQuizPanel(this::handleCreateQuestion, this::handleCancelToQuestions, topics);
                },
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
            view.showEditQuizPanel(this::handleCreateQuestion, this::handleCancelToQuestions, topics);
            view.topicsCombo.setSelectedItem(questionToEdit.getTopic());
            view.questionField.setText(questionToEdit.getQuestionText());
            String[] answers = questionToEdit.getAnswers();
            for (int i = 0; i < 4; i++) {
                view.answerFields[i].setText(answers[i]);
            }
            view.correctAnswerCombo.setSelectedIndex(questionToEdit.getCorrectAnswer());
        }
    }

    private void handleCreateQuestion(ActionEvent e) {
        createOrUpdateQuestion(null);
    }

    private void createOrUpdateQuestion(Question existingQuestion) {
        String topic = (String) view.topicsCombo.getSelectedItem();
        String questionText = view.questionField.getText();
        String[] answers = new String[4];
        for (int i = 0; i < 4; i++) {
            answers[i] = view.answerFields[i].getText();
        }
        int correctAnswer = view.correctAnswerCombo.getSelectedIndex();

        Question question = existingQuestion != null ? existingQuestion : new Question();
        if (existingQuestion == null) {
            question.setQuestionId(new Random().nextInt(10000));
        }
        question.setQuestionText(questionText);
        question.setAnswers(answers);
        question.setCorrectAnswer(correctAnswer);
        question.setTopic(topic);

        Database.addOrUpdateQuestion(question);
        JOptionPane.showMessageDialog(null, "Frage wurde gespeichert!");
        startApplication();
    }

    private void handleStartQuiz(ActionEvent e) {
        String username = view.newUserField.getText();
        if (!username.isEmpty()) {
            currentUser = new User(username);
        } else {
            String selectedUser = (String) view.existingUserCombo.getSelectedItem();
            currentUser = Database.loadUsers().stream()
                    .filter(u -> u.getUsername().equals(selectedUser))
                    .findFirst()
                    .orElse(null);
        }

        if (currentUser == null) {
            JOptionPane.showMessageDialog(null, "Bitte Benutzer auswählen oder anlegen!");
            return;
        }

        if (!currentUser.getWrongQuestionCounts().isEmpty()) {
            currentQuestions = currentUser.getTopWorstQuestions(allQuestions);
            JOptionPane.showMessageDialog(null, "Top 10 Worst Questions werden geübt!");
        } else {
            String selectedTopic = (String) view.topicSelectionCombo.getSelectedItem();
            currentQuestions = allQuestions.stream()
                    .filter(q -> q.getTopic().equals(selectedTopic))
                    .collect(Collectors.toList());
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
            JOptionPane.showMessageDialog(null, "Falsch! Richtige Antwort: " + question.getAnswers()[question.getCorrectAnswer()]);
            wrongQuestions.add(question);
            currentUser.addWrongAnswer(question);
        }

        askNextQuestion();
    }

    private void handleCancel(ActionEvent e) {
        startApplication();
    }

    private void handleCancelToMain(ActionEvent e) {
        startApplication();
    }

    private void handleCancelToQuestions(ActionEvent e) {
        showQuestionsForEditing();
    }
}

