package l.halbritter.View;

import javax.swing.*;
import java.awt.*;

public class QuizView {

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JTextArea textArea;

    public QuizView() {
        frame = new JFrame("Quiz Application");
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        textArea = new JTextArea(5, 40);
        textArea.setEditable(false);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    public void createAndShowGUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null); // zentriert das Fenster auf dem Bildschirm

        JPanel mainMenuPanel = createMainMenuPanel();
        JPanel editQuizPanel = createEditQuizPanel();
        JPanel startQuizPanel = createStartQuizPanel();

        JScrollPane scrollPane = new JScrollPane(textArea);

        cardPanel.add(mainMenuPanel, "MainMenu");
        cardPanel.add(editQuizPanel, "EditQuiz");
        cardPanel.add(startQuizPanel, "StartQuiz");

        frame.add(cardPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        frame.setVisible(true);
        cardLayout.show(cardPanel, "MainMenu");
    }

    private JPanel createMainMenuPanel() {
        JPanel panel = createCenteredPanel();

        panel.add(createButton("1. Quiz bearbeiten", e -> cardLayout.show(cardPanel, "EditQuiz")));
        panel.add(createButton("2. Quiz starten", e -> cardLayout.show(cardPanel, "StartQuiz")));
        panel.add(createButton("3. Beenden", e -> System.exit(0)));

        return panel;
    }

    private JPanel createEditQuizPanel() {
        JPanel panel = createCenteredPanel();

        panel.add(createButton("1. Themenbereich auswählen", e -> showText("Themenbereich auswählen...")));
        panel.add(createButton("2. Neue Frage erstellen", e -> createNewQuestion()));
        panel.add(createButton("Zurück zum Hauptmenü", e -> cardLayout.show(cardPanel, "MainMenu")));

        return panel;
    }

    private JPanel createStartQuizPanel() {
        JPanel panel = createCenteredPanel();

        panel.add(createButton("1. Neuen Benutzer anlegen", e -> createNewUser()));
        panel.add(createButton("2. Bestehenden Benutzer auswählen", e -> selectExistingUser()));
        panel.add(createButton("Zurück zum Hauptmenü", e -> cardLayout.show(cardPanel, "MainMenu")));

        return panel;
    }

    private JButton createButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("SansSerif", Font.PLAIN, 16));
        button.setMaximumSize(new Dimension(250, 40));
        button.addActionListener(action);
        return button;
    }

    private JPanel createCenteredPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));
        return panel;
    }

    private void createNewQuestion() {
        String questionText = JOptionPane.showInputDialog(frame, "Frage eingeben:");
        String[] answers = new String[4];
        for (int i = 0; i < 4; i++) {
            answers[i] = JOptionPane.showInputDialog(frame, "Antwort " + (i + 1) + ":");
        }
        String correctAnswer = JOptionPane.showInputDialog(frame, "Richtige Antwort (1-4):");
        String difficulty = JOptionPane.showInputDialog(frame, "Schwierigkeitsgrad (1-3):");

        showText("Neue Frage erstellt:\n" + questionText);
    }

    private void createNewUser() {
        String username = JOptionPane.showInputDialog(frame, "Benutzernamen eingeben:");
        showText("Benutzer '" + username + "' wurde erstellt.");
    }

    private void selectExistingUser() {
        String[] users = {"Alice", "Bob", "Charlie"};

        JComboBox<String> userDropdown = new JComboBox<>(users);
        userDropdown.setSelectedIndex(0);

        int result = JOptionPane.showConfirmDialog(
                frame,
                userDropdown,
                "Benutzer auswählen",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String selectedUser = (String) userDropdown.getSelectedItem();
            showText("Benutzer '" + selectedUser + "' ausgewählt.");
        }
    }

    private void showText(String text) {
        textArea.setText(text);
    }
}
