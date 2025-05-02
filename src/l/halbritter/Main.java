package l.halbritter;

import l.halbritter.Controller.QuizController;
import l.halbritter.Database.Database;

public class Main {
    public static void main(String[] args) {
        Database.connect();
        new QuizController().startApplication();
    }
}
