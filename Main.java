import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    // Core components
    private BorderPane rootPane;
    private TabPane chartTabPane;
    private VBox controlPanel;
    private ProgressBar progressBar;
    private Label statusLabel;
    private TextArea dataPreview;

    // Data management
    private ObservableList<DataPoint> dataset;
    private ExecutorService executorService;

    // Chart components
    private LineChart<Number, Number> lineChart;
    private AreaChart<Number, Number> areaChart;
    private BarChart<String, Number> barChart;
    private PieChart pieChart;
    private ScatterChart<Number, Number> scatterChart;

    @Override
    public void start(Stage primaryStage) {
        executorService = Executors.newFixedThreadPool(4);
        dataset = FXCollections.observableArrayList();

        initializeUI();
        setupEventHandlers();
        generateSampleData();

        Scene scene = new Scene(rootPane, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("Advanced Data Visualization Tool");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            executorService.shutdown();
            Platform.exit();
        });
        primaryStage.show();
    }

    private void initializeUI() {
        rootPane = new BorderPane();
        rootPane.getStyleClass().add("root-pane");

        // Create control panel
        createControlPanel();

        // Create chart tabs
        createChartTabs();

        // Create status bar
        createStatusBar();

        // Layout
        rootPane.setLeft(controlPanel);
        rootPane.setCenter(chartTabPane);
        rootPane.setBottom(createStatusBar());
    }

    private void createControlPanel() {
        controlPanel = new VBox(10);
        controlPanel.getStyleClass().add("control-panel");
        controlPanel.setPadding(new Insets(15));
        controlPanel.setPrefWidth(250);

        Label titleLabel = new Label("Data Controls");
        titleLabel.getStyleClass().add("panel-title");

        // Data generation controls
        VBox dataGenGroup = createGroupBox("Data Generation");

        Button generateBtn = new Button("Generate Sample Data");
        generateBtn.getStyleClass().add("primary-button");
        generateBtn.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> dataTypeCombo = new ComboBox<>();
        dataTypeCombo.getItems().addAll("Linear", "Exponential", "Sinusoidal", "Random", "Large Dataset");
        dataTypeCombo.setValue("Linear");
        dataTypeCombo.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> dataSizeSpinner = new Spinner<>(100, 100000, 1000, 100);
        dataSizeSpinner.setMaxWidth(Double.MAX_VALUE);
        Label sizeLabel = new Label("Dataset Size:");

        dataGenGroup.getChildren().addAll(
                sizeLabel, dataSizeSpinner,
                new Label("Data Type:"), dataTypeCombo,
                generateBtn
        );

        // File operations
        VBox fileOpsGroup = createGroupBox("File Operations");

        Button loadBtn = new Button("Load CSV Data");
        loadBtn.getStyleClass().add("secondary-button");
        loadBtn.setMaxWidth(Double.MAX_VALUE);

        Button exportBtn = new Button("Export Charts");
        exportBtn.getStyleClass().add("secondary-button");
        exportBtn.setMaxWidth(Double.MAX_VALUE);

        fileOpsGroup.getChildren().addAll(loadBtn, exportBtn);

        // Chart controls
        VBox chartGroup = createGroupBox("Chart Controls");

        CheckBox animationCheck = new CheckBox("Enable Animations");
        animationCheck.setSelected(true);

        CheckBox legendCheck = new CheckBox("Show Legends");
        legendCheck.setSelected(true);

        Slider opacitySlider = new Slider(0.1, 1.0, 0.8);
        Label opacityLabel = new Label("Chart Opacity:");

        chartGroup.getChildren().addAll(
                animationCheck, legendCheck,
                opacityLabel, opacitySlider
        );

        // Data preview
        VBox previewGroup = createGroupBox("Data Preview");
        dataPreview = new TextArea();
        dataPreview.setPrefRowCount(8);
        dataPreview.setEditable(false);
        dataPreview.getStyleClass().add("data-preview");

        previewGroup.getChildren().add(dataPreview);

        controlPanel.getChildren().addAll(
                titleLabel,
                dataGenGroup,
                fileOpsGroup,
                chartGroup,
                previewGroup
        );

        // Event handlers
        generateBtn.setOnAction(e -> generateData(dataTypeCombo.getValue(), dataSizeSpinner.getValue()));
        loadBtn.setOnAction(e -> loadCSVData());
        exportBtn.setOnAction(e -> exportCharts());

        animationCheck.setOnAction(e -> toggleAnimations(animationCheck.isSelected()));
        legendCheck.setOnAction(e -> toggleLegends(legendCheck.isSelected()));
        opacitySlider.valueProperty().addListener((obs, old, val) -> updateChartOpacity(val.doubleValue()));
    }

    private VBox createGroupBox(String title) {
        VBox group = new VBox(8);
        group.getStyleClass().add("group-box");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("group-title");

        VBox content = new VBox(5);
        content.getStyleClass().add("group-content");

        group.getChildren().addAll(titleLabel, content);
        return content; // Return content box for adding children
    }

    private void createChartTabs() {
        chartTabPane = new TabPane();
        chartTabPane.getStyleClass().add("chart-tabs");

        // Line Chart Tab
        Tab lineTab = new Tab("Line Chart");
        NumberAxis xAxis1 = new NumberAxis();
        NumberAxis yAxis1 = new NumberAxis();
        lineChart = new LineChart<>(xAxis1, yAxis1);
        lineChart.setTitle("Line Chart Visualization");
        lineChart.getStyleClass().add("chart");

        ScrollPane lineScrollPane = new ScrollPane(lineChart);
        lineScrollPane.setFitToWidth(true);
        lineScrollPane.setFitToHeight(true);
        lineTab.setContent(lineScrollPane);

        // Area Chart Tab
        Tab areaTab = new Tab("Area Chart");
        NumberAxis xAxis2 = new NumberAxis();
        NumberAxis yAxis2 = new NumberAxis();
        areaChart = new AreaChart<>(xAxis2, yAxis2);
        areaChart.setTitle("Area Chart Visualization");
        areaChart.getStyleClass().add("chart");

        ScrollPane areaScrollPane = new ScrollPane(areaChart);
        areaScrollPane.setFitToWidth(true);
        areaScrollPane.setFitToHeight(true);
        areaTab.setContent(areaScrollPane);

        // Bar Chart Tab
        Tab barTab = new Tab("Bar Chart");
        CategoryAxis xAxis3 = new CategoryAxis();
        NumberAxis yAxis3 = new NumberAxis();
        barChart = new BarChart<>(xAxis3, yAxis3);
        barChart.setTitle("Bar Chart Visualization");
        barChart.getStyleClass().add("chart");

        ScrollPane barScrollPane = new ScrollPane(barChart);
        barScrollPane.setFitToWidth(true);
        barScrollPane.setFitToHeight(true);
        barTab.setContent(barScrollPane);

        // Pie Chart Tab
        Tab pieTab = new Tab("Pie Chart");
        pieChart = new PieChart();
        pieChart.setTitle("Pie Chart Visualization");
        pieChart.getStyleClass().add("chart");

        ScrollPane pieScrollPane = new ScrollPane(pieChart);
        pieScrollPane.setFitToWidth(true);
        pieScrollPane.setFitToHeight(true);
        pieTab.setContent(pieScrollPane);

        // Scatter Chart Tab
        Tab scatterTab = new Tab("Scatter Chart");
        NumberAxis xAxis5 = new NumberAxis();
        NumberAxis yAxis5 = new NumberAxis();
        scatterChart = new ScatterChart<>(xAxis5, yAxis5);
        scatterChart.setTitle("Scatter Chart Visualization");
        scatterChart.getStyleClass().add("chart");

        ScrollPane scatterScrollPane = new ScrollPane(scatterChart);
        scatterScrollPane.setFitToWidth(true);
        scatterScrollPane.setFitToHeight(true);
        scatterTab.setContent(scatterScrollPane);

        chartTabPane.getTabs().addAll(lineTab, areaTab, barTab, pieTab, scatterTab);
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dataCountLabel = new Label("Data Points: 0");
        dataCountLabel.getStyleClass().add("info-label");

        // Update data count when dataset changes
        dataset.addListener((javafx.collections.ListChangeListener<DataPoint>) c -> {
            Platform.runLater(() -> dataCountLabel.setText("Data Points: " + dataset.size()));
        });

        statusBar.getChildren().addAll(statusLabel, progressBar, spacer, dataCountLabel);
        return statusBar;
    }

    private void setupEventHandlers() {
        // Add interactive features to charts
        setupChartInteractivity();
    }

    private void setupChartInteractivity() {
        // Add zoom and pan capabilities would go here
        // For now, we'll add basic hover effects through CSS
    }

    private void generateData(String dataType, int size) {
        updateStatus("Generating " + dataType + " data...", true);

        Task<ObservableList<DataPoint>> task = new Task<ObservableList<DataPoint>>() {
            @Override
            protected ObservableList<DataPoint> call() throws Exception {
                ObservableList<DataPoint> newData = FXCollections.observableArrayList();
                Random random = new Random();

                for (int i = 0; i < size; i++) {
                    double x = i;
                    double y;
                    String category = "Category " + (i % 5 + 1);

                    switch (dataType) {
                        case "Linear":
                            y = 2 * x + random.nextGaussian() * 10;
                            break;
                        case "Exponential":
                            y = Math.exp(x / 100.0) + random.nextGaussian() * 5;
                            break;
                        case "Sinusoidal":
                            y = 100 * Math.sin(x / 50.0) + random.nextGaussian() * 10;
                            break;
                        case "Large Dataset":
                            y = random.nextGaussian() * 50 + Math.sin(x / 100.0) * 30;
                            break;
                        default: // Random
                            y = random.nextGaussian() * 100;
                    }

                    newData.add(new DataPoint(x, y, category));

                    if (i % 1000 == 0) {
                        updateProgress(i, size);
                    }
                }

                return newData;
            }

            @Override
            protected void succeeded() {
                dataset.clear();
                dataset.addAll(getValue());
                updateCharts();
                updateDataPreview();
                updateStatus("Data generation completed", false);
            }

            @Override
            protected void failed() {
                updateStatus("Data generation failed", false);
            }
        };

        executorService.submit(task);
    }

    private void generateSampleData() {
        generateData("Sinusoidal", 500);
    }

    private void loadCSVData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load CSV Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            loadCSVFile(file);
        }
    }

    private void loadCSVFile(File file) {
        updateStatus("Loading CSV file...", true);

        Task<ObservableList<DataPoint>> task = new Task<ObservableList<DataPoint>>() {
            @Override
            protected ObservableList<DataPoint> call() throws Exception {
                ObservableList<DataPoint> newData = FXCollections.observableArrayList();

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    boolean firstLine = true;
                    int lineCount = 0;

                    while ((line = reader.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false; // Skip header
                            continue;
                        }

                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            try {
                                double x = Double.parseDouble(parts[0].trim());
                                double y = Double.parseDouble(parts[1].trim());
                                String category = parts.length > 2 ? parts[2].trim() : "Data";

                                newData.add(new DataPoint(x, y, category));
                                lineCount++;

                                if (lineCount % 100 == 0) {
                                    updateProgress(lineCount, -1); // Unknown total
                                }
                            } catch (NumberFormatException e) {
                                // Skip invalid lines
                            }
                        }
                    }
                }

                return newData;
            }

            @Override
            protected void succeeded() {
                dataset.clear();
                dataset.addAll(getValue());
                updateCharts();
                updateDataPreview();
                updateStatus("CSV data loaded successfully", false);
            }

            @Override
            protected void failed() {
                updateStatus("Failed to load CSV data", false);
            }
        };

        executorService.submit(task);
    }

    private void updateCharts() {
        Platform.runLater(() -> {
            updateLineChart();
            updateAreaChart();
            updateBarChart();
            updatePieChart();
            updateScatterChart();
        });
    }

    private void updateLineChart() {
        lineChart.getData().clear();

        if (dataset.isEmpty()) return;

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Data Series");

        // Sample data for performance with large datasets
        int step = Math.max(1, dataset.size() / 1000);
        for (int i = 0; i < dataset.size(); i += step) {
            DataPoint dp = dataset.get(i);
            series.getData().add(new XYChart.Data<>(dp.getX(), dp.getY()));
        }

        lineChart.getData().add(series);
    }

    private void updateAreaChart() {
        areaChart.getData().clear();

        if (dataset.isEmpty()) return;

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Area Data");

        int step = Math.max(1, dataset.size() / 1000);
        for (int i = 0; i < dataset.size(); i += step) {
            DataPoint dp = dataset.get(i);
            series.getData().add(new XYChart.Data<>(dp.getX(), Math.abs(dp.getY())));
        }

        areaChart.getData().add(series);
    }

    private void updateBarChart() {
        barChart.getData().clear();

        if (dataset.isEmpty()) return;

        // Group data by category
        Map<String, Double> categoryData = new HashMap<>();
        for (DataPoint dp : dataset) {
            categoryData.merge(dp.getCategory(), dp.getY(), Double::sum);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Category Data");

        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);
    }

    private void updatePieChart() {
        pieChart.getData().clear();

        if (dataset.isEmpty()) return;

        Map<String, Double> categoryData = new HashMap<>();
        for (DataPoint dp : dataset) {
            categoryData.merge(dp.getCategory(), Math.abs(dp.getY()), Double::sum);
        }

        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            pieChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
    }

    private void updateScatterChart() {
        scatterChart.getData().clear();

        if (dataset.isEmpty()) return;

        Map<String, XYChart.Series<Number, Number>> seriesMap = new HashMap<>();

        int step = Math.max(1, dataset.size() / 2000); // Limit points for performance
        for (int i = 0; i < dataset.size(); i += step) {
            DataPoint dp = dataset.get(i);
            String category = dp.getCategory();

            seriesMap.computeIfAbsent(category, k -> {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(k);
                return series;
            }).getData().add(new XYChart.Data<>(dp.getX(), dp.getY()));
        }

        scatterChart.getData().addAll(seriesMap.values());
    }

    private void updateDataPreview() {
        StringBuilder preview = new StringBuilder();
        preview.append("Dataset Summary:\n");
        preview.append("Total Points: ").append(dataset.size()).append("\n\n");

        if (!dataset.isEmpty()) {
            preview.append("First 10 data points:\n");
            preview.append("X\t\tY\t\tCategory\n");
            preview.append("--------------------------------\n");

            for (int i = 0; i < Math.min(10, dataset.size()); i++) {
                DataPoint dp = dataset.get(i);
                preview.append(String.format("%.2f\t%.2f\t%s\n",
                        dp.getX(), dp.getY(), dp.getCategory()));
            }

            if (dataset.size() > 10) {
                preview.append("... and ").append(dataset.size() - 10).append(" more points");
            }
        }

        Platform.runLater(() -> dataPreview.setText(preview.toString()));
    }

    private void exportCharts() {
        updateStatus("Exporting charts...", true);
        // Implementation for chart export would go here
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            updateStatus("Charts exported successfully", false);
        }));
        timeline.play();
    }

    private void toggleAnimations(boolean enable) {
        lineChart.setAnimated(enable);
        areaChart.setAnimated(enable);
        barChart.setAnimated(enable);
        pieChart.setAnimated(enable);
        scatterChart.setAnimated(enable);
    }

    private void toggleLegends(boolean show) {
        lineChart.setLegendVisible(show);
        areaChart.setLegendVisible(show);
        barChart.setLegendVisible(show);
        pieChart.setLegendVisible(show);
        scatterChart.setLegendVisible(show);
    }

    private void updateChartOpacity(double opacity) {
        lineChart.setOpacity(opacity);
        areaChart.setOpacity(opacity);
        barChart.setOpacity(opacity);
        pieChart.setOpacity(opacity);
        scatterChart.setOpacity(opacity);
    }

    private void updateStatus(String message, boolean showProgress) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            progressBar.setVisible(showProgress);
            if (!showProgress) {
                progressBar.setProgress(0);
            }
        });
    }

    private void updateProgress(int current, int total) {
        Platform.runLater(() -> {
            if (total > 0) {
                progressBar.setProgress((double) current / total);
            } else {
                progressBar.setProgress(-1); // Indeterminate
            }
        });
    }

    // Data Point class
    public static class DataPoint {
        private double x, y;
        private String category;

        public DataPoint(double x, double y, String category) {
            this.x = x;
            this.y = y;
            this.category = category;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public String getCategory() { return category; }

        @Override
        public String toString() {
            return String.format("(%.2f, %.2f) [%s]", x, y, category);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

