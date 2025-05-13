package l.halbritter.View;

import l.halbritter.Model.Question;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class QuizUI {
    private JFrame frame;
    private JPanel mainPanel;

    // Shared components
    public JButton addTopicButton = new JButton("Thema hinzufügen");
    public JButton deleteTopicButton = new JButton("Thema löschen");
    public JButton deletePlayerButton;


    public JComboBox<String> topicsCombo;
    public JTextField questionField;
    public JTextField[] answerFields;
    public JComboBox<String> difficultyCombo;
    public JComboBox<String> correctAnswerCombo;

    public JComboBox<String> topicSelectionCombo;
    public JComboBox<String> difficultySelectionCombo;

    public JList<String> userList;
    public JButton newPlayerButton;
    public JButton backToMainButton;

    private final Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private final Font buttonFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    private final Insets panelInsets = new Insets(10, 10, 10, 10);

    public QuizUI() {
        // System Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        frame = new JFrame("Quiz Anwendung");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Empfohlene Grundgröße
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        mainPanel = new JPanel(new CardLayout());
        mainPanel.setBorder(new EmptyBorder(panelInsets));
        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }

    private void configureButton(JButton btn) {
        btn.setFont(buttonFont);
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(8, 16, 8, 16));
    }

    /** Main Menu **/
    public void showMainMenu(
            ActionListener editListener,
            ActionListener startQuizListener,
            ActionListener showWorstListener,
            ActionListener exitListener
    ) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(panelInsets));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Hauptmenü");
        title.setFont(titleFont);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        JButton editBtn = new JButton("Fragen bearbeiten");
        JButton startBtn = new JButton("Quiz starten");
        JButton worstBtn = new JButton("10 schlechteste Fragen");
        JButton exitBtn = new JButton("Beenden");

        for (JButton btn : new JButton[]{editBtn, startBtn, worstBtn, exitBtn}) {
            configureButton(btn);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(btn);
            panel.add(Box.createVerticalStrut(10));
        }

        editBtn.addActionListener(editListener);
        startBtn.addActionListener(startQuizListener);
        worstBtn.addActionListener(showWorstListener);
        exitBtn.addActionListener(exitListener);

        mainPanel.add(panel, "mainMenu");
        switchTo("mainMenu");
    }

    /** Nutzerauswahl für schlechteste Fragen **/
    public void showUserSelectionPanel(
            ActionListener userSelectedListener,
            ActionListener cancelListener,
            List<String> existingUsers
    ) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(panelInsets));

        userList = new JList<>(existingUsers.toArray(new String[0]));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(buttonFont);
        JScrollPane scroll = new JScrollPane(userList);
        scroll.setBorder(new TitledBorder("Spieler auswählen"));

        userList.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                userSelectedListener.actionPerformed(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selected)
                );
            }
        });

        backToMainButton = new JButton("Zurück");
        configureButton(backToMainButton);
        backToMainButton.addActionListener(cancelListener);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(backToMainButton, BorderLayout.SOUTH);

        mainPanel.add(panel, "userSelect");
        switchTo("userSelect");
    }

    /** Spieler-Auswahl vor Quiz **/
    public void showPlayerSelectionPanel(
            ActionListener newPlayerListener,
            ActionListener existingPlayerListener,
            ActionListener deletePlayerListener,
            ActionListener cancelListener,
            List<String> existingUsers
    ) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(panelInsets));

        JLabel title = new JLabel("Spieler auswählen oder neu anlegen");
        title.setFont(titleFont);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        userList = new JList<>(existingUsers.toArray(new String[0]));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(buttonFont);
        JScrollPane scroll = new JScrollPane(userList);
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // nur bei echtem Doppelklick reagieren
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();  // weiter Doppelerkennung unterbinden
                    // aktuelles Element auslesen
                    String selected = userList.getSelectedValue();
                    if (selected != null) {
                        // genau so, wie der ListSelectionListener vorher, das
                        // ActionEvent an den Controller schicken:
                        existingPlayerListener.actionPerformed(
                                new ActionEvent(
                                        this,
                                        ActionEvent.ACTION_PERFORMED,
                                        selected
                                )
                        );
                    }
                }
            }
        });

        scroll.setBorder(new TitledBorder("Existierende Spieler"));

        newPlayerButton = new JButton("Neuen Spieler anlegen");
        configureButton(newPlayerButton);
        newPlayerButton.addActionListener(newPlayerListener);

        deletePlayerButton = new JButton("Spieler löschen");
        configureButton(deletePlayerButton);
        deletePlayerButton.addActionListener(deletePlayerListener);

        backToMainButton = new JButton("Zurück");
        configureButton(backToMainButton);
        backToMainButton.addActionListener(cancelListener);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(newPlayerButton);
        south.add(deletePlayerButton);
        south.add(backToMainButton);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);

        mainPanel.add(panel, "playerSelect");
        switchTo("playerSelect");
    }

    /** Fragen nach Thema listen **/
    public void showQuestionListPanel(Map<String, List<Question>> questionsByTopic,
                                      ActionListener editQuestionListener,
                                      ActionListener newQuestionListener,
                                      ActionListener cancelListener) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(panelInsets));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        for (String topic : questionsByTopic.keySet()) {
            JLabel topicLabel = new JLabel(topic);
            topicLabel.setFont(buttonFont.deriveFont(Font.BOLD, 18f));
            topicLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
            listPanel.add(topicLabel);
            for (Question q : questionsByTopic.get(topic)) {
                JButton qBtn = new JButton(q.getQuestionText());
                configureButton(qBtn);
                qBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                qBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
                qBtn.setActionCommand(String.valueOf(q.getQuestionId()));
                qBtn.addActionListener(editQuestionListener);
                listPanel.add(qBtn);
                listPanel.add(Box.createVerticalStrut(5));
            }
            listPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(new TitledBorder("Fragenübersicht"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton newBtn = new JButton("Neue Frage erstellen");
        configureButton(newBtn);
        newBtn.addActionListener(newQuestionListener);
        bottom.add(newBtn);

        configureButton(addTopicButton);
        bottom.add(addTopicButton);
        configureButton(deleteTopicButton);
        bottom.add(deleteTopicButton);

        JButton backBtn = new JButton("Zurück");
        configureButton(backBtn);
        backBtn.addActionListener(cancelListener);
        bottom.add(backBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        mainPanel.add(panel, "questionList");
        switchTo("questionList");
    }

    /** Frage erstellen/bearbeiten **/
    public void showEditQuizPanel(ActionListener saveListener,
                                  ActionListener deleteListener,
                                  ActionListener cancelListener,
                                  List<String> topics) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(panelInsets));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Thema
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Thema:"), gbc);
        topicsCombo = new JComboBox<>(topics.toArray(new String[0]));
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(topicsCombo, gbc);

        // Frage
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Frage:"), gbc);
        questionField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(questionField, gbc);

        // Antworten
        answerFields = new JTextField[4];
        for (int i = 0; i < 4; i++) {
            gbc.gridx = 0; gbc.gridy = 2 + i;
            panel.add(new JLabel("Antwort " + (i + 1) + ":"), gbc);
            answerFields[i] = new JTextField();
            gbc.gridx = 1; gbc.gridy = 2 + i;
            panel.add(answerFields[i], gbc);
        }

        // Schwierigkeit
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Schwierigkeit:"), gbc);
        difficultyCombo = new JComboBox<>(new String[]{"1", "2", "3"});
        gbc.gridx = 1; gbc.gridy = 6;
        panel.add(difficultyCombo, gbc);

        // Richtige Antwort
        gbc.gridx = 0; gbc.gridy = 7;
        panel.add(new JLabel("Richtige Antwort:"), gbc);
        correctAnswerCombo = new JComboBox<>(new String[]{"1", "2", "3", "4"});
        gbc.gridx = 1; gbc.gridy = 7;
        panel.add(correctAnswerCombo, gbc);

        // Buttons
         gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
         gbc.gridwidth = 2;

        // Button-Panel: Speichern, ggf. Frage löschen, Abbrechen
        JPanel btnPanel = new JPanel();

        JButton saveBtn   = new JButton("Speichern");
        configureButton(saveBtn);
        saveBtn.addActionListener(saveListener);
        btnPanel.add(saveBtn);

        // <<< NUR HINZUFÜGEN, wenn ein deleteListener da ist >>>
        if (deleteListener != null) {
            JButton deleteBtn = new JButton("Frage löschen");
            configureButton(deleteBtn);
            deleteBtn.addActionListener(deleteListener);
            btnPanel.add(deleteBtn);
        }

        JButton cancelBtn = new JButton("Abbrechen");
        configureButton(cancelBtn);
        cancelBtn.addActionListener(cancelListener);
        btnPanel.add(cancelBtn);

        panel.add(btnPanel, gbc);

        mainPanel.add(panel, "editQuiz");
        switchTo("editQuiz");
    }

    /** Quiz-Auswahl **/
    public void showQuizSelectionPanel(ActionListener startQuizListener,
                                       ActionListener cancelListener,
                                       List<String> topics) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(panelInsets));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Thema auswählen
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Thema wählen:"), gbc);
        topicSelectionCombo = new JComboBox<>(topics.toArray(new String[0]));
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(topicSelectionCombo, gbc);

        // Schwierigkeit auswählen
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Schwierigkeit:"), gbc);
        difficultySelectionCombo = new JComboBox<>(new String[]{"1", "2", "3"});
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(difficultySelectionCombo, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        JPanel btnP = new JPanel();
        JButton startBtn = new JButton("Quiz starten");
        JButton cancelBtn3 = new JButton("Abbrechen");
        configureButton(startBtn);
        configureButton(cancelBtn3);
        startBtn.addActionListener(startQuizListener);
        cancelBtn3.addActionListener(cancelListener);
        btnP.add(startBtn);
        btnP.add(cancelBtn3);
        panel.add(btnP, gbc);

        mainPanel.add(panel, "quizSelect");
        switchTo("quizSelect");
    }

    private void switchTo(String name) {
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, name);
    }
}