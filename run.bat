@echo off
setlocal

:: Config
set JAVAFX_PATH=lib\javafx-sdk-24.0.2\lib
set MYSQL_DRIVER=lib\mysql-connector-j-9.3.0.jar
set MAIN_CLASS=com.votreport.App

:: Compilation
javac --module-path %JAVAFX_PATH% --add-modules javafx.controls,javafx.fxml ^
      -d target\classes ^
      src\main\java\com\votreport\App.java ^
      src\main\java\com\votreport\controllers\*.java ^
      src\main\java\com\votreport\DBConnection.java

:: Copie ressources
xcopy /Y /E /I src\main\resources\com\votreport\views\* target\classes\com\votreport\views\

:: Exécution
java --module-path %JAVAFX_PATH% --add-modules javafx.controls,javafx.fxml ^
     -cp "%MYSQL_DRIVER%;target\classes" %MAIN_CLASS%

pause