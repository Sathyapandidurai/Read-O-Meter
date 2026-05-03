import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================
 *  Read-O-Meter Pro — MVP
 *  Author : <Your Name>
 *  Java   : 17+
 *  Pattern: MVC-lite  (View + Controller in separate inner classes)
 * ============================================================
 *
 *  ARCHITECTURE OVERVIEW (great talking point in interviews!):
 *  ┌──────────────────────────────────────────────────────────┐
 *  │  ReadOMeterApp   – JavaFX entry-point, wires everything  │
 *  │  AppView         – builds the UI (View responsibility)   │
 *  │  TextController  – pure logic, NO JavaFX imports needed  │
 *  └──────────────────────────────────────────────────────────┘
 */
public class ReadOMeterApp extends Application {

    // ── entry-point ──────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Wire the controller and the view together
        TextController controller = new TextController();
        AppView view = new AppView(controller);

        Scene scene = new Scene(view.buildRoot(), 900, 620);

        // Load external CSS for a modern dark-mode look
        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("style.css"),
                "style.css not found – make sure it is in the same folder as the .java file"
            ).toExternalForm()
        );

        primaryStage.setTitle("📖 Read-O-Meter Pro");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(460);
        primaryStage.show();
    }

    // ══════════════════════════════════════════════════════════
    //  VIEW  –  builds the entire JavaFX layout
    // ══════════════════════════════════════════════════════════
    static class AppView {

        private final TextController controller;

        // Stat-card labels we need to update from the controller callback
        private Label readingTimeValue;
        private Label wordCountValue;
        private Label complexityValue;
        private Label tiredWordsValue;
        private Label statusLabel;

        AppView(TextController controller) {
            this.controller = controller;
        }

        /**
         * Assembles and returns the root BorderPane that backs the Scene.
         * Call once from start().
         */
        BorderPane buildRoot() {
            BorderPane root = new BorderPane();
            root.getStyleClass().add("root-pane");

            root.setTop(buildHeader());
            root.setCenter(buildTextArea());
            root.setRight(buildSidebar());
            root.setBottom(buildStatusBar());

            return root;
        }

        // ── HEADER ────────────────────────────────────────────
        private HBox buildHeader() {
            Label title = new Label("Read-O-Meter Pro");
            title.getStyleClass().add("app-title");

            Label subtitle = new Label("Paste, type, or drop a .txt file to analyse your writing");
            subtitle.getStyleClass().add("app-subtitle");

            VBox titleBox = new VBox(2, title, subtitle);

            HBox header = new HBox(titleBox);
            header.getStyleClass().add("header");
            header.setAlignment(Pos.CENTER_LEFT);
            return header;
        }

        // ── TEXT AREA (centre) ────────────────────────────────
        private StackPane buildTextArea() {
            TextArea textArea = new TextArea();
            textArea.setPromptText("✍  Start typing — or drag & drop a .txt file here …");
            textArea.setWrapText(true);
            textArea.getStyleClass().add("main-textarea");

            // Tooltip explains drag-and-drop to the user
            Tooltip dndTip = new Tooltip(
                "💡 Drag & drop a plain-text (.txt) file anywhere onto this area\n" +
                "   to load it instantly. Statistics update in real time."
            );
            dndTip.setShowDelay(javafx.util.Duration.millis(400));
            Tooltip.install(textArea, dndTip);

            // ── Async real-time stats ──────────────────────────
            // We use a small delay so we don't hammer the CPU on every keystroke.
            // javafx.animation.PauseTransition gives us a clean debounce pattern.
            javafx.animation.PauseTransition debounce =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(350));

            textArea.textProperty().addListener((obs, oldText, newText) -> {
                debounce.setOnFinished(e -> runAnalysisAsync(newText));
                debounce.playFromStart();          // restart the countdown on each keystroke
            });

            // ── Drag-over (visual feedback) ────────────────────
            textArea.setOnDragOver(event -> {
                if (event.getGestureSource() != textArea
                        && event.getDragboard().hasFiles()) {
                    // Signal that we can accept a COPY of the dragged item
                    event.acceptTransferModes(TransferMode.COPY);
                    textArea.getStyleClass().add("drag-over");   // highlight border via CSS
                }
                event.consume();
            });

            // Remove the highlight when the drag leaves
            textArea.setOnDragExited(event -> {
                textArea.getStyleClass().remove("drag-over");
                event.consume();
            });

            // ── Drag-dropped (async file I/O) ──────────────────
            textArea.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasFiles()) {
                    List<File> files = db.getFiles();
                    File droppedFile = files.getFirst();  // Java 21 list accessor (use get(0) on Java 17)

                    if (droppedFile.getName().endsWith(".txt")) {
                        /*
                         *  KEY INTERVIEW POINT:
                         *  We use a JavaFX Task (wraps Runnable) to read the file
                         *  on a background thread so the UI thread is NEVER blocked.
                         *  Platform.runLater() marshals the result back to the FX thread.
                         */
                        Task<String> loadFileTask = new Task<>() {
                            @Override
                            protected String call() throws IOException {
                                updateMessage("Loading file…");
                                return Files.readString(droppedFile.toPath());
                            }
                        };

                        // On success → push text to TextArea on FX thread
                        loadFileTask.setOnSucceeded(e -> {
                            String content = loadFileTask.getValue();
                            Platform.runLater(() -> {
                                textArea.setText(content);
                                setStatus("✅  Loaded: " + droppedFile.getName());
                            });
                        });

                        // On failure → show a friendly error
                        loadFileTask.setOnFailed(e -> {
                            Throwable ex = loadFileTask.getException();
                            Platform.runLater(() ->
                                setStatus("❌  Error reading file: " + ex.getMessage())
                            );
                        });

                        // Bind the status bar to the task message property
                        statusLabel.textProperty().bind(loadFileTask.messageProperty());

                        // Spin up a daemon thread (dies when the app closes)
                        Thread fileThread = new Thread(loadFileTask);
                        fileThread.setDaemon(true);
                        fileThread.start();

                        success = true;
                    } else {
                        setStatus("⚠️  Only .txt files are supported.");
                    }
                }

                event.setDropCompleted(success);
                event.consume();
            });

            // Wrap in a StackPane so CSS padding works cleanly
            StackPane wrapper = new StackPane(textArea);
            wrapper.getStyleClass().add("textarea-wrapper");
            StackPane.setMargin(textArea, new Insets(0));
            return wrapper;
        }

        // ── RIGHT SIDEBAR ─────────────────────────────────────
        private VBox buildSidebar() {
            Label sidebarTitle = new Label("📊 Analysis");
            sidebarTitle.getStyleClass().add("sidebar-title");

            // Build each stat card
            readingTimeValue = new Label("—");
            wordCountValue   = new Label("—");
            complexityValue  = new Label("—");
            tiredWordsValue  = new Label("—");

            VBox sidebar = new VBox(14,
                sidebarTitle,
                buildCard("⏱  Reading Time",  readingTimeValue),
                buildCard("🔢  Word Count",    wordCountValue),
                buildCard("🎓  Complexity",    complexityValue),
                buildCard("🚩  Tired Words",   tiredWordsValue),
                buildLegend()
            );

            sidebar.getStyleClass().add("sidebar");
            sidebar.setPrefWidth(220);
            sidebar.setMinWidth(200);
            return sidebar;
        }

        /**
         * Factory that produces a labelled "stat card" VBox.
         * Each card has a title label and a dynamic value label.
         */
        private VBox buildCard(String title, Label valueLabel) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("card-title");

            valueLabel.getStyleClass().add("card-value");

            VBox card = new VBox(4, titleLabel, valueLabel);
            card.getStyleClass().add("stat-card");
            return card;
        }

        /** Small legend explaining complexity thresholds */
        private VBox buildLegend() {
            Label heading = new Label("Complexity Guide");
            heading.getStyleClass().add("legend-heading");

            Label easy   = new Label("🟢 Easy   — avg word ≤ 5 chars");
            Label medium = new Label("🟡 Medium — avg word ≤ 7 chars");
            Label hard   = new Label("🔴 Hard   — avg word  > 7 chars");

            for (Label l : List.of(easy, medium, hard)) {
                l.getStyleClass().add("legend-item");
            }

            VBox legend = new VBox(4, heading, easy, medium, hard);
            legend.getStyleClass().add("legend-box");
            return legend;
        }

        // ── STATUS BAR (bottom) ───────────────────────────────
        private HBox buildStatusBar() {
            statusLabel = new Label("Ready. Paste text or drop a .txt file to begin.");
            statusLabel.getStyleClass().add("status-label");

            HBox bar = new HBox(statusLabel);
            bar.getStyleClass().add("status-bar");
            bar.setAlignment(Pos.CENTER_LEFT);
            return bar;
        }

        // ── ASYNC ANALYSIS HELPER ─────────────────────────────
        /**
         * Runs TextController analysis on a background thread and
         * pushes the results back to the UI via Platform.runLater().
         *
         * INTERVIEW TALKING POINT:
         *   "I deliberately separated the computation from the UI update.
         *    The Task runs on a worker thread so the FX Application Thread
         *    is never blocked, keeping the UI silky-smooth even for large files."
         */
        private void runAnalysisAsync(String text) {
            Task<TextController.AnalysisResult> task = new Task<>() {
                @Override
                protected TextController.AnalysisResult call() {
                    return controller.analyse(text);
                }
            };

            task.setOnSucceeded(e -> {
                TextController.AnalysisResult result = task.getValue();
                Platform.runLater(() -> updateStats(result));
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        }

        /** Pushes a fresh AnalysisResult into the stat-card labels. */
        private void updateStats(TextController.AnalysisResult r) {
            readingTimeValue.setText(r.readingTime());
            wordCountValue.setText(String.valueOf(r.wordCount()));

            complexityValue.setText(r.complexityLevel());
            // Colour the complexity label dynamically
            complexityValue.getStyleClass().removeAll("easy", "medium", "hard");
            complexityValue.getStyleClass().add(r.complexityLevel().toLowerCase());

            tiredWordsValue.setText(r.tiredWordSummary());
            setStatus("✔  Analysis complete — " + r.wordCount() + " words found.");
        }

        private void setStatus(String message) {
            // Unbind first in case the Task bound it earlier
            statusLabel.textProperty().unbind();
            statusLabel.setText(message);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  CONTROLLER  –  pure business logic, zero JavaFX imports
    // ══════════════════════════════════════════════════════════
    static class TextController {

        /** Words per minute assumed for an average adult reader */
        private static final int WPM = 260;

        /** Words we consider "tired" or overused */
        private static final Set<String> TIRED_WORDS = Set.of(
            "very", "just", "really", "quite", "basically",
            "literally", "actually", "stuff", "things", "nice"
        );

        /**
         * Immutable record carrying all statistics for a single text snapshot.
         *
         * INTERVIEW TALKING POINT:
         *   "I used a Java 16+ record as the return type. Records give me
         *    immutability, auto-generated equals/hashCode/toString, and
         *    a concise syntax — perfect for data carriers."
         */
        record AnalysisResult(
            int    wordCount,
            String readingTime,
            String complexityLevel,
            String tiredWordSummary
        ) {}

        /**
         * Analyses the supplied text and returns an AnalysisResult.
         * This method is pure — it has no side-effects and is safe to call
         * from any thread (which is why we call it from a background Task).
         *
         * @param text the raw text from the TextArea
         * @return a fully populated AnalysisResult
         */
        AnalysisResult analyse(String text) {
            if (text == null || text.isBlank()) {
                return new AnalysisResult(0, "—", "—", "—");
            }

            String[] words = tokenise(text);
            int count            = words.length;
            String readingTime   = calculateReadingTime(count);
            String complexity    = calculateComplexity(words);
            String tiredSummary  = findTiredWords(words);

            return new AnalysisResult(count, readingTime, complexity, tiredSummary);
        }

        // ── private helpers ───────────────────────────────────

        /** Splits text into individual word tokens, stripping punctuation. */
        private String[] tokenise(String text) {
            return Arrays.stream(text.split("\\s+"))
                         .map(w -> w.replaceAll("[^a-zA-Z']", "").toLowerCase())
                         .filter(w -> !w.isEmpty())
                         .toArray(String[]::new);
        }

        /**
         * Calculates reading time based on a constant WPM rate.
         * Returns a human-friendly string like "2 min 15 sec".
         */
        private String calculateReadingTime(int wordCount) {
            if (wordCount == 0) return "—";
            int totalSeconds = (int) Math.ceil((double) wordCount / WPM * 60);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            if (minutes == 0) return seconds + " sec";
            if (seconds == 0) return minutes + " min";
            return minutes + " min " + seconds + " sec";
        }

        /**
         * Assigns a complexity level based on average word length.
         * Thresholds are intentionally simple for an MVP — easy to extend.
         */
        private String calculateComplexity(String[] words) {
            if (words.length == 0) return "—";

            double avgLen = Arrays.stream(words)
                                  .mapToInt(String::length)
                                  .average()
                                  .orElse(0.0);

            if (avgLen <= 5.0) return "Easy";
            if (avgLen <= 7.0) return "Medium";
            return "Hard";
        }

        /**
         * Scans for tired / overused words and returns a short summary.
         * Uses a frequency map so we can list each offending word once.
         */
        private String findTiredWords(String[] words) {
            Map<String, Long> freq = Arrays.stream(words)
                .filter(TIRED_WORDS::contains)
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

            if (freq.isEmpty()) return "✅ None found!";

            // Build "very(3), just(1)" style summary
            String summary = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.joining(", "));

            // Truncate if too long for the card
            return summary.length() > 28 ? summary.substring(0, 25) + "…" : summary;
        }
    }
}
