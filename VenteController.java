package com.votreport.controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javafx.beans.property.StringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;


public class VenteController {

    @FXML private ComboBox<String> filterTypeComboBox;
    @FXML private TextField filterValueTextField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<Vente> ventesTable;
    @FXML private TableColumn<Vente, Integer> colIdVente;
    @FXML private TableColumn<Vente, String> colProduit;
    @FXML private TableColumn<Vente, String> colSousEntite;
    @FXML private TableColumn<Vente, Integer> colQuantite;
    @FXML private TableColumn<Vente, Double> colPrixUnitaire;
    @FXML private TableColumn<Vente, Double> colMontantTotal;
    @FXML private TableColumn<Vente, String> colDateVente;
    @FXML private TableColumn<Vente, String> colVenduPar;
    @FXML private Label totalAmountLabel;
    @FXML private BarChart<String, Number> salesChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private VBox chartContainer;

    private ObservableList<Vente> ventesData = FXCollections.observableArrayList();
    private FilteredList<Vente> filteredVentes;
    private Controller mainController;

    public void setMainController(Controller mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // Configure table columns
        colIdVente.setCellValueFactory(cellData -> cellData.getValue().idVenteProperty().asObject());
        colProduit.setCellValueFactory(cellData -> cellData.getValue().produitProperty());
        colSousEntite.setCellValueFactory(cellData -> cellData.getValue().sousEntiteProperty());
        colQuantite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty().asObject());
        colPrixUnitaire.setCellValueFactory(cellData -> cellData.getValue().prixUnitaireProperty().asObject());
        colMontantTotal.setCellValueFactory(cellData -> cellData.getValue().montantTotalProperty().asObject());
        colDateVente.setCellValueFactory(cellData -> cellData.getValue().dateVenteProperty());
        colVenduPar.setCellValueFactory(cellData -> cellData.getValue().venduParProperty());

        // Format montant total
        colMontantTotal.setCellFactory(column -> new TableCell<Vente, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : String.format("%.2f €", item));
            }
        });

        // Format prix unitaire
        colPrixUnitaire.setCellFactory(column -> new TableCell<Vente, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : String.format("%.2f €", item));
            }
        });

        // Load data
        loadVentes();

        // Initialize filtered list
        filteredVentes = new FilteredList<>(ventesData, p -> true);
        SortedList<Vente> sortedData = new SortedList<>(filteredVentes);
        sortedData.comparatorProperty().bind(ventesTable.comparatorProperty());
        ventesTable.setItems(sortedData);

        // Update total amount
        updateTotalAmount();

        // Listen to filter type changes
        filterTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            filterValueTextField.setVisible(!newVal.equals("Entre deux dates"));
            startDatePicker.setVisible(newVal.equals("Entre deux dates"));
            endDatePicker.setVisible(newVal.equals("Entre deux dates"));
        });
    }

    private void loadVentes() {
        ventesData.clear();
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "")) {
            String sql = "SELECT v.id_vente, p.designation AS produit, se.nom AS sous_entite, v.quantite, " +
                         "v.prix_unitaire, v.montant_total, v.date_vente, v.vendu_par " +
                         "FROM Vente v " +
                         "JOIN Produit p ON v.id_produit = p.id_produit " +
                         "JOIN SousEntite se ON v.id_sous_entite = se.id_sous_entite";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                ventesData.add(new Vente(
                    rs.getInt("id_vente"),
                    rs.getString("produit"),
                    rs.getString("sous_entite"),
                    rs.getInt("quantite"),
                    rs.getDouble("prix_unitaire"),
                    rs.getDouble("montant_total"),
                    rs.getTimestamp("date_vente").toString(),
                    rs.getString("vendu_par")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void applyFilter(ActionEvent event) {
        String filterType = filterTypeComboBox.getValue();
        if (filterType == null) return;

        filteredVentes.setPredicate(vente -> {
            if (filterType.equals("Produit")) {
                String filterValue = filterValueTextField.getText().toLowerCase();
                return vente.getProduit().toLowerCase().contains(filterValue);
            } else if (filterType.equals("Sous-entité")) {
                String filterValue = filterValueTextField.getText().toLowerCase();
                return vente.getSousEntite().toLowerCase().contains(filterValue);
            } else if (filterType.equals("Quantité")) {
                try {
                    int filterValue = Integer.parseInt(filterValueTextField.getText());
                    return vente.getQuantite() == filterValue;
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (filterType.equals("Prix unitaire")) {
                try {
                    double filterValue = Double.parseDouble(filterValueTextField.getText());
                    return vente.getPrixUnitaire() == filterValue;
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (filterType.equals("Date de vente")) {
                try {
                    LocalDate filterDate = LocalDate.parse(filterValueTextField.getText());
                    LocalDate venteDate = LocalDate.parse(vente.getDateVente().substring(0, 10));
                    return venteDate.equals(filterDate);
                } catch (Exception e) {
                    return false;
                }
            } else if (filterType.equals("Entre deux dates")) {
                LocalDate start = startDatePicker.getValue();
                LocalDate end = endDatePicker.getValue();
                if (start == null || end == null) return false;

                try {
                    LocalDate venteDate = LocalDate.parse(vente.getDateVente().substring(0, 10));
                    return (venteDate.isEqual(start) || venteDate.isAfter(start)) && 
                           (venteDate.isEqual(end) || venteDate.isBefore(end));
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        });
        updateTotalAmount();
    }

    @FXML
    private void resetFilter(ActionEvent event) {
        filterTypeComboBox.getSelectionModel().clearSelection();
        filterValueTextField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        filteredVentes.setPredicate(null);
        updateTotalAmount();
    }

    private void updateTotalAmount() {
        double total = 0.0;
        for (Vente vente : filteredVentes) {
            total += vente.getMontantTotal();
        }
        totalAmountLabel.setText(String.format("%.2f €", total));
    }

    @FXML
    private void showChart(ActionEvent event) {
        // Clear previous data
        salesChart.getData().clear();

        // Group sales by product
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Montant total");

        for (Vente vente : filteredVentes) {
            boolean found = false;
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getXValue().equals(vente.getProduit())) {
                    data.setYValue(data.getYValue().doubleValue() + vente.getMontantTotal());
                    found = true;
                    break;
                }
            }
            if (!found) {
                series.getData().add(new XYChart.Data<>(vente.getProduit(), vente.getMontantTotal()));
            }
        }

        salesChart.getData().add(series);
        chartContainer.setVisible(true);
    }

    public static class Vente {
        private final IntegerProperty idVente;
        private final StringProperty produit;
        private final StringProperty sousEntite;
        private final IntegerProperty quantite;
        private final DoubleProperty prixUnitaire;
        private final DoubleProperty montantTotal;
        private final StringProperty dateVente;
        private final StringProperty venduPar;

        public Vente(int idVente, String produit, String sousEntite, int quantite, 
                     double prixUnitaire, double montantTotal, String dateVente, String venduPar) {
            this.idVente = new SimpleIntegerProperty(idVente);
            this.produit = new SimpleStringProperty(produit);
            this.sousEntite = new SimpleStringProperty(sousEntite);
            this.quantite = new SimpleIntegerProperty(quantite);
            this.prixUnitaire = new SimpleDoubleProperty(prixUnitaire);
            this.montantTotal = new SimpleDoubleProperty(montantTotal);
            this.dateVente = new SimpleStringProperty(dateVente);
            this.venduPar = new SimpleStringProperty(venduPar);
        }

        // Getters for properties
        public IntegerProperty idVenteProperty() { return idVente; }
        public StringProperty produitProperty() { return produit; }
        public StringProperty sousEntiteProperty() { return sousEntite; }
        public IntegerProperty quantiteProperty() { return quantite; }
        public DoubleProperty prixUnitaireProperty() { return prixUnitaire; }
        public DoubleProperty montantTotalProperty() { return montantTotal; }
        public StringProperty dateVenteProperty() { return dateVente; }
        public StringProperty venduParProperty() { return venduPar; }

        // Getters for values
        public int getIdVente() { return idVente.get(); }
        public String getProduit() { return produit.get(); }
        public String getSousEntite() { return sousEntite.get(); }
        public int getQuantite() { return quantite.get(); }
        public double getPrixUnitaire() { return prixUnitaire.get(); }
        public double getMontantTotal() { return montantTotal.get(); }
        public String getDateVente() { return dateVente.get(); }
        public String getVenduPar() { return venduPar.get(); }
    }
}