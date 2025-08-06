package com.votreport.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.*;
import java.sql.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;


public class SousEntiteController {

    @FXML private TableView<SousEntite> sousEntiteTable;
    @FXML private TableColumn<SousEntite, Integer> colId;
    @FXML private TableColumn<SousEntite, String> colNom;
    @FXML private TableColumn<SousEntite, String> colAdresse;
    @FXML private TableColumn<SousEntite, String> colResponsable;
    @FXML private TableColumn<SousEntite, Void> colActions;

    private ObservableList<SousEntite> sousEntiteData = FXCollections.observableArrayList();
    private SousEntite newSousEntiteRow = new SousEntite(-1, "", "", "");
    private Controller mainController;

    public void setMainController(Controller controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colNom.setCellValueFactory(cellData -> cellData.getValue().nomProperty());
        colAdresse.setCellValueFactory(cellData -> cellData.getValue().adresseProperty());
        colResponsable.setCellValueFactory(cellData -> cellData.getValue().responsableProperty());

        // Rendre les colonnes éditables
        colNom.setCellFactory(TextFieldTableCell.forTableColumn());
        colNom.setOnEditCommit(event -> {
            SousEntite se = event.getRowValue();
            se.setNom(event.getNewValue());
            saveOrUpdateSousEntite(se);
        });

        colAdresse.setCellFactory(TextFieldTableCell.forTableColumn());
        colAdresse.setOnEditCommit(event -> {
            SousEntite se = event.getRowValue();
            se.setAdresse(event.getNewValue());
            saveOrUpdateSousEntite(se);
        });

        colResponsable.setCellFactory(TextFieldTableCell.forTableColumn());
        colResponsable.setOnEditCommit(event -> {
            SousEntite se = event.getRowValue();
            se.setResponsable(event.getNewValue());
            saveOrUpdateSousEntite(se);
        });

        // Colonne Actions
        colActions.setCellFactory(param -> new TableCell<SousEntite, Void>() {
        private final Button btnDelete = new Button("✕");
        private final Button btnViewStock = new Button("Voir stock");
        private final HBox container = new HBox(5, btnViewStock, btnDelete);
    
    {
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(event -> {
            SousEntite sousEntite = getTableView().getItems().get(getIndex());
            deleteSousEntite(sousEntite);
        });
        
        btnViewStock.getStyleClass().add("view-button");
        btnViewStock.setOnAction(event -> {
            SousEntite sousEntite = getTableView().getItems().get(getIndex());
            mainController.showStockSousEntite(sousEntite.getId());
        });
    }
    
    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : container);
    }
});

        sousEntiteTable.setItems(sousEntiteData);
        sousEntiteTable.setEditable(true);
        sousEntiteData.add(newSousEntiteRow);
        sousEntiteTable.setOnKeyPressed(this::handleKeyPressed);
        loadSousEntites();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            SousEntite selected = sousEntiteTable.getSelectionModel().getSelectedItem();
            if (selected != null) saveOrUpdateSousEntite(selected);
        }
    }

    private void loadSousEntites() {
        sousEntiteData.clear();
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM SousEntite ORDER BY nom")) {
            
            while (rs.next()) {
                sousEntiteData.add(new SousEntite(
                    rs.getInt("id_sous_entite"),
                    rs.getString("nom"),
                    rs.getString("adresse"),
                    rs.getString("responsable")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sousEntiteData.add(newSousEntiteRow);
    }

    private void saveOrUpdateSousEntite(SousEntite sousEntite) {
        if (sousEntite.getId() == -1) {
            if (!sousEntite.getNom().isEmpty()) saveNewSousEntite(sousEntite);
        } else {
            updateSousEntiteInDatabase(sousEntite);
        }
    }

    private void saveNewSousEntite(SousEntite sousEntite) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO SousEntite (nom, adresse, responsable) VALUES (?, ?, ?)", 
                 Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, sousEntite.getNom());
            pstmt.setString(2, sousEntite.getAdresse());
            pstmt.setString(3, sousEntite.getResponsable());
            pstmt.executeUpdate();
            
            newSousEntiteRow.setNom("");
            newSousEntiteRow.setAdresse("");
            newSousEntiteRow.setResponsable("");
            loadSousEntites();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSousEntiteInDatabase(SousEntite sousEntite) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE SousEntite SET nom = ?, adresse = ?, responsable = ? WHERE id_sous_entite = ?")) {
            
            pstmt.setString(1, sousEntite.getNom());
            pstmt.setString(2, sousEntite.getAdresse());
            pstmt.setString(3, sousEntite.getResponsable());
            pstmt.setInt(4, sousEntite.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSousEntite(SousEntite sousEntite) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer : " + sousEntite.getNom());
        alert.setContentText("Êtes-vous sûr ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
                     PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM SousEntite WHERE id_sous_entite = ?")) {
                    
                    pstmt.setInt(1, sousEntite.getId());
                    pstmt.executeUpdate();
                    loadSousEntites();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static class SousEntite {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty nom = new SimpleStringProperty();
        private final StringProperty adresse = new SimpleStringProperty();
        private final StringProperty responsable = new SimpleStringProperty();

        public SousEntite(int id, String nom, String adresse, String responsable) {
            this.id.set(id);
            this.nom.set(nom);
            this.adresse.set(adresse);
            this.responsable.set(responsable);
        }

        // Getters
        public int getId() { return id.get(); }
        public String getNom() { return nom.get(); }
        public String getAdresse() { return adresse.get(); }
        public String getResponsable() { return responsable.get(); }

        // Setters
        public void setId(int id) { this.id.set(id); }
        public void setNom(String nom) { this.nom.set(nom); }
        public void setAdresse(String adresse) { this.adresse.set(adresse); }
        public void setResponsable(String responsable) { this.responsable.set(responsable); }

        // Property getters
        public IntegerProperty idProperty() { return id; }
        public StringProperty nomProperty() { return nom; }
        public StringProperty adresseProperty() { return adresse; }
        public StringProperty responsableProperty() { return responsable; }
    }
}




///////////
// Modifier la colonne Actions dans SousEntiteController.java
