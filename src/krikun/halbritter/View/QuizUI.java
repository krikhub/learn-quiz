/*
 * © 2025 Luca Halbritter und Alexander Krikun
 * IU Internationale Hochschule, Hamburg
 * SPDX-License-Identifier: MIT
 */
package krikun.halbritter.View;

import krikun.halbritter.Model.Question;

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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        frame = new JFrame("Quiz Anwendung");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        mainPanel = new JPanel(new CardLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }
    
    public JFrame getFrame() {
        return frame;
    }

    private void configureButton(JButton btn) {
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
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
        JButton worstBtn = new JButton("Top 10 schlechteste Fragen");
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
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(title);

        JLabel instructionLabel = new JLabel("Doppelklick auf Nutzer oder neuen erstellen, um Quiz zu starten");
        instructionLabel.setFont(buttonFont);
        instructionLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        northPanel.add(instructionLabel);

        panel.add(northPanel, BorderLayout.NORTH);

        userList = new JList<>(existingUsers.toArray(new String[0]));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(buttonFont);
        JScrollPane scroll = new JScrollPane(userList);
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();  
                    String selected = userList.getSelectedValue();
                    if (selected != null) {
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
    public void showQuestionListPanel(
            Map<String, List<Question>> questionsByTopic,
            ActionListener editListener,
            ActionListener createListener,
            ActionListener cancelListener
    ) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        configureButton(addTopicButton);
        configureButton(deleteTopicButton);
        toolBar.add(addTopicButton);
        toolBar.addSeparator(new Dimension(10, 0));
        toolBar.add(deleteTopicButton);
        panel.add(toolBar, BorderLayout.NORTH);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        for (String topic : questionsByTopic.keySet()) {
            List<Question> qs = questionsByTopic.get(topic);
            
            JPanel topicPanel = new JPanel();
            topicPanel.setLayout(new BoxLayout(topicPanel, BoxLayout.Y_AXIS));
            topicPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            for (Question q : qs) {
                JButton btn = new JButton(q.getQuestionText());
                configureButton(btn);
                btn.setAlignmentX(Component.LEFT_ALIGNMENT);
                btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
                btn.setActionCommand(String.valueOf(q.getQuestionId()));
                btn.addActionListener(editListener);
                topicPanel.add(btn);
                topicPanel.add(Box.createRigidArea(new Dimension(0,5)));
            }

            JScrollPane scroll = new JScrollPane(topicPanel,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            tabbedPane.addTab(topic, scroll);
        }
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        JPanel bottomBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton newQuestionBtn = new JButton("Neue Frage");
        configureButton(newQuestionBtn);
        if (createListener != null) {
            newQuestionBtn.addActionListener(createListener);
            newQuestionBtn.setEnabled(true);
        } else {
            newQuestionBtn.setEnabled(false);
        }
        bottomBtnPanel.add(newQuestionBtn);

        JButton cancelBtn = new JButton("Zurück");
        configureButton(cancelBtn);
        cancelBtn.addActionListener(cancelListener);
        bottomBtnPanel.add(cancelBtn);

        panel.add(bottomBtnPanel, BorderLayout.SOUTH);
        
        mainPanel.add(panel, "questionList");
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, "questionList");
    }

    /** Frage erstellen/bearbeiten **/
    public void showEditQuizPanel(ActionListener saveListener,
                                  ActionListener deleteListener,
                                  ActionListener cancelListener,
                                  List<String> topics) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Thema:"), gbc);
        topicsCombo = new JComboBox<>(topics.toArray(new String[0]));
        topicsCombo.setToolTipText("Thema auswählen");
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(topicsCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Frage:"), gbc);
        questionField = new JTextField();
        questionField.setToolTipText("Hier Frage eingeben…");
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(questionField, gbc);
        
        answerFields = new JTextField[4];
        for (int i = 0; i < 4; i++) {
            gbc.gridx = 0; gbc.gridy = 2 + i;
            panel.add(new JLabel("Antwort " + (i + 1) + ":"), gbc);
            answerFields[i] = new JTextField();
            answerFields[i].setToolTipText("Antwort " + (i + 1) + " eingeben");
            gbc.gridx = 1; gbc.gridy = 2 + i;
            panel.add(answerFields[i], gbc);
        }
        
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Schwierigkeit:"), gbc);
        difficultyCombo = new JComboBox<>(new String[]{"1", "2", "3"});
        difficultyCombo.setToolTipText("Schwierigkeit auswählen");
        gbc.gridx = 1; gbc.gridy = 6;
        panel.add(difficultyCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 7;
        panel.add(new JLabel("Richtige Antwort:"), gbc);
        correctAnswerCombo = new JComboBox<>(new String[]{"1", "2", "3", "4"});
        correctAnswerCombo.setToolTipText("Richtige Antwort auswählen");
        gbc.gridx = 1; gbc.gridy = 7;
        panel.add(correctAnswerCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel();

        JButton saveBtn = new JButton("Speichern");
        configureButton(saveBtn);
        saveBtn.addActionListener(saveListener);
        btnPanel.add(saveBtn);

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
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, "editQuiz");
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
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Thema wählen:"), gbc);
        topicSelectionCombo = new JComboBox<>(topics.toArray(new String[0]));
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(topicSelectionCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Schwierigkeit:"), gbc);
        difficultySelectionCombo = new JComboBox<>(new String[]{"1", "2", "3"});
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(difficultySelectionCombo, gbc);
        
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