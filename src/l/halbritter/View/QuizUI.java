package l.halbritter.View;

import l.halbritter.Model.Question;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizUI {
    private JFrame frame;
    private JPanel mainPanel;
    public JButton addTopicButton = new JButton("Thema hinzufügen");
    public JButton deleteTopicButton = new JButton("Thema löschen");

    public JComboBox<String> topicsCombo;
    public JTextField questionField;
    public JTextField[] answerFields;
    public JComboBox<String> difficultyCombo;
    public JComboBox<String> correctAnswerCombo;

    public JTextField newUserField;
    public JComboBox<String> existingUserCombo;
    public JComboBox<String> topicSelectionCombo;
    public JComboBox<String> difficultySelectionCombo;

    public JList<String> userList;
    public JButton newPlayerButton;
    public JButton backToMainButton;

    public QuizUI() {
        frame = new JFrame("Quiz Anwendung");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setLocationRelativeTo(null);
        mainPanel = new JPanel(new BorderLayout());
        frame.getContentPane().add(mainPanel);
    }

    public void showMainMenu(ActionListener editQuizListener, ActionListener startQuizListener, ActionListener exitListener) {
        mainPanel.removeAll();

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));

        JButton editQuizButton = new JButton("1. Quiz bearbeiten");
        JButton startQuizButton = new JButton("2. Quiz starten");
        JButton exitButton = new JButton("3. Beenden");

        editQuizButton.addActionListener(editQuizListener);
        startQuizButton.addActionListener(startQuizListener);
        exitButton.addActionListener(exitListener);

        buttonPanel.add(editQuizButton);
        buttonPanel.add(startQuizButton);
        buttonPanel.add(exitButton);

        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        frame.revalidate();
        frame.repaint();
        frame.setVisible(true);
    }

    public void showQuestionListPanel(Map<String, List<Question>> questionsByTopic,
                                      ActionListener editQuestionListener,
                                      ActionListener newQuestionListener,
                                      ActionListener cancelListener) {
        mainPanel.removeAll();

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        for (String topic : questionsByTopic.keySet()) {
            JLabel topicLabel = new JLabel("Thema: " + topic);
            topicLabel.setFont(new Font("Arial", Font.BOLD, 16));
            listPanel.add(topicLabel);

            for (Question question : questionsByTopic.get(topic)) {
                JButton questionButton = new JButton(question.getQuestionText());
                questionButton.setActionCommand(String.valueOf(question.getQuestionId()));
                questionButton.addActionListener(editQuestionListener);
                listPanel.add(questionButton);
            }
        }

        JButton createNewButton = new JButton("Neue Frage erstellen");
        createNewButton.addActionListener(newQuestionListener);

        JButton cancelButton = new JButton("Zurück");
        cancelButton.addActionListener(cancelListener);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(createNewButton);
        bottomPanel.add(addTopicButton);
        bottomPanel.add(deleteTopicButton);
        bottomPanel.add(cancelButton);

        JScrollPane scrollPane = new JScrollPane(listPanel);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();
    }

    public void showEditQuizPanel(ActionListener createQuestionListener, ActionListener cancelListener, List<String> topics) {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(12, 1, 5, 5));
        topicsCombo = new JComboBox<>(topics.toArray(new String[0]));
        questionField = new JTextField();
        answerFields = new JTextField[]{new JTextField(), new JTextField(), new JTextField(), new JTextField()};
        difficultyCombo = new JComboBox<>(new String[]{"1", "2", "3"});
        correctAnswerCombo = new JComboBox<>(new String[]{"Antwort 1", "Antwort 2", "Antwort 3", "Antwort 4"});

        panel.add(new JLabel("Themenbereich wählen:"));
        panel.add(topicsCombo);

        panel.add(new JLabel("Frage eingeben:"));
        panel.add(questionField);
        for (int i = 0; i < 4; i++) {
            panel.add(new JLabel("Antwort " + (i + 1) + ":"));
            panel.add(answerFields[i]);
        }
        panel.add(new JLabel("Schwierigkeit auswählen:"));
        panel.add(difficultyCombo);
        panel.add(new JLabel("Richtige Antwort auswählen:"));
        panel.add(correctAnswerCombo);

        JButton createButton = new JButton("Frage erstellen");
        JButton cancelButton = new JButton("Abbrechen");

        createButton.addActionListener(createQuestionListener);
        cancelButton.addActionListener(cancelListener);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();
    }


    public void showPlayerSelectionPanel(
            ActionListener newPlayerListener,
            ActionListener existingPlayerListener,
            ActionListener cancelListener,
            List<String> existingUsers
    ) {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout(10,10));

        // 1) Liste der existierenden Spieler
        userList = new JList<>(existingUsers.toArray(new String[0]));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(userList);

        // Wenn in der Liste auf einen Eintrag geklickt wird:
        userList.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                // Feuer ein ActionEvent mit dem Spielernamen als command
                existingPlayerListener.actionPerformed(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selected)
                );
            }
        });

        // 2) Button „Neuen Spieler anlegen“
        newPlayerButton = new JButton("Neuen Spieler anlegen");
        newPlayerButton.addActionListener(newPlayerListener);

        // 3) „Zurück“-Button
        backToMainButton = new JButton("Zurück");
        backToMainButton.addActionListener(cancelListener);

        // Layout
        JPanel top = new JPanel(new BorderLayout(5,5));
        top.add(newPlayerButton, BorderLayout.NORTH);
        top.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(backToMainButton);

        mainPanel.add(top,    BorderLayout.CENTER);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();
    }


    public void showQuizSelectionPanel(ActionListener startQuizListener,
                                       ActionListener cancelListener,
                                       List<String> topics) {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());

        topicSelectionCombo      = new JComboBox<>(topics.toArray(new String[0]));
        difficultySelectionCombo = new JComboBox<>(new String[]{"1", "2", "3"});

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Thema auswählen:"));
        panel.add(topicSelectionCombo);
        panel.add(new JLabel("Schwierigkeit auswählen:"));
        panel.add(difficultySelectionCombo);

        JButton startButton  = new JButton("Quiz starten");
        JButton cancelButton = new JButton("Abbrechen");
        startButton.addActionListener(startQuizListener);
        cancelButton.addActionListener(cancelListener);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();
    }
}