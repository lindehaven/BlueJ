 

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Vector;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author ericjbruno
 */
public class JavaFXSimpleEditor extends Application {
    private static final String BROWSER = "Browser";
    private static final String EDITOR = "new editor";
    private static int browserCnt = 1;

    private Stage primaryStage;
    private TabPane tabPane;
    private Vector<SimpleEditor> editors = new Vector();
    private SimpleEditor currentEditor = null;
    static boolean ignoreNextPress = false;
    
    private Stage getStage() {
        return primaryStage;
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Add an empty editor to the tab pane
        tabPane = new TabPane();
        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
                @Override public void changed(ObservableValue<? extends Tab> tab, Tab oldTab, Tab newTab) { 
                    // As the current tab changes, reset the var that tracks
                    // the editor in view. This is used for tracking modified
                    // editors as the user types
                    currentEditor = null;
                }
            });
        
        // Create main app menu
        MenuBar menuBar = new MenuBar();
        
        // File menu and subitems
        Menu menuFile = new Menu("File");
        MenuItem menuFileNew = new MenuItem("New");
        menuFileNew.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                createNew(EDITOR);
            }
        });
        MenuItem menuFileOpen = new MenuItem("Open");
        menuFileOpen.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                chooseAndLoadFile();
            }
        });
        MenuItem menuFileSave = new MenuItem("Save");
        menuFileSave.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                saveFileRev();
            }
        });
        MenuItem menuFileExit = new MenuItem("Exit");
        menuFileExit.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                stop();
                getStage().close();
            }
        });
        
        menuFile.getItems().addAll(
                menuFileNew,
                menuFileOpen,
                menuFileSave,
                new SeparatorMenuItem(), 
                menuFileExit);
        
        Menu menuView = new Menu("View");
        MenuItem menuViewURL = new MenuItem("Web Page");
        menuViewURL.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                createNew(BROWSER);
            }
        });
        menuView.getItems().addAll(menuViewURL);
        menuBar.getMenus().addAll(menuFile, menuView);
        
        // layout the scene
        VBox layout = VBoxBuilder.create().spacing(10).children(menuBar, tabPane).build();
        layout.setFillWidth(true);
        
        // display the scene
        final Scene scene = new Scene(layout, 800, 600);
        // Bind the tab pane width/height to the scene
        tabPane.prefWidthProperty().bind(scene.widthProperty());
        tabPane.prefHeightProperty().bind(scene.heightProperty());

        // Certain keys only come through on key release events
        // such as backspace, enter, and delete
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
                public void handle(KeyEvent ke) {
                    String text = ke.getText();
                    KeyCode code = ke.getCode();
                    System.out.println("onKeyPressed: code="+code+", text="+text);
                    handleKeyPress(ke);
                }
            });

        scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
                public void handle(KeyEvent ke) {
                    String text = ke.getText();
                    KeyCode code = ke.getCode();
                    System.out.println("onKeyReleased: code="+code+", text="+text);
                    if ( code == KeyCode.BACK_SPACE || 
                         code == KeyCode.ENTER ||
                         code == KeyCode.DELETE ) {
                        indicateFileModified();
                    }
                    
                    // After the "s" is pressed to invoke a save action, make
                    // sure the subsequent release doesn't mark the file
                    // to be saved once again
                    if ( ! (ke.isControlDown() || ke.isMetaDown()) ) {
                        if ( text.equals("s") && ignoreNextPress ) {
                            ignoreNextPress = false;
                            return;
                        }
                        handleKeyPress(ke);
                    }
                }
            });

//        scene.setOnKeyTyped(new EventHandler<KeyEvent>() {
//                public void handle(KeyEvent ke) {
//                    handleKeyPress(ke);
//                }
//            });
//
        // Make sure one new editor is open by default
        createNew(EDITOR);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Simple Editor / Browser");
        primaryStage.show();
    }
    
    private void createNew(String type) {
        Tab tab = new Tab();
        Content content = null;
        
        switch ( type ) {
        case EDITOR:
            content = new SimpleEditor();
            editors.add((SimpleEditor)content);
            break;
        case BROWSER:
            content = new WebBrowser();
            type += (browserCnt++);
            break;
        }

        tab.setContent(content.getRoot());
        tab.setText(type);
        tabPane.getTabs().add(tab);
                SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
                selectionModel.select(tab);
    }

    private void indicateFileModified() {
        if ( currentEditor != null && currentEditor.modified ) {
            return;
        }
        
        // Get current tab, add an "*" to its name to indicate modified
        System.out.println("Indicating text modified");
        SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
        Tab selectedTab = selectionModel.getSelectedItem();
        TextArea area = (TextArea)selectedTab.getContent();
        currentEditor = getEditorForTextArea(area);
        String modName = selectedTab.getText();
        if ( ! modName.endsWith("*") ) {
            modName += "*";
            selectedTab.setText(modName);
        }
        currentEditor.modified = true;
    }
    
    private SimpleEditor getEditorForTextArea(TextArea area) {
        Iterator<SimpleEditor> iter = editors.iterator();
        while ( iter.hasNext() ) {
            SimpleEditor editor = iter.next();
            if ( area == (TextArea)editor.getRoot() )
                return editor;
        }
        
        return null;
    }
    
    private void chooseAndLoadFile() {
        FileChooser fc = new FileChooser();
        File fileToOpen = fc.showOpenDialog(null);
        if ( fileToOpen != null ) {
            // Read the file, and set its contents within the editor
            String openFileName = fileToOpen.getAbsolutePath();
            StringBuffer sb = new StringBuffer();
            try ( FileInputStream fis = new FileInputStream(fileToOpen);
                  BufferedInputStream bis = new BufferedInputStream(fis) ) {
                while ( bis.available() > 0 ) {
                    sb.append((char)bis.read());
                }
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }

            // Create the editor with this content and store it
            SimpleEditor editor = new SimpleEditor();
            editor.setText( sb.toString() );
            editor.filename = openFileName;
            editors.add(editor);

            // Create a tab to house the new editor
            Tab tab = new Tab();
            tab.setText(fileToOpen.getName());
            tab.setContent(editor.getRoot());
            tabPane.getTabs().add(tab);        
            
            // Make sure the new tab is selected
            SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
            selectionModel.select(tab);
        }
    }

    private void saveFileRev() {
        System.out.println("saving file");
        boolean success = false;
        SimpleEditor editor = null;
        File file = null;

        SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
        Tab selectedTab = selectionModel.getSelectedItem();
        editor = getEditorForTextArea((TextArea)selectedTab.getContent());
        if ( editor == null )
            return;
        String openFileName = editor.filename;

        if ( openFileName == null ) {
            // No file was opened. The user just started typing
            // Save new file now
            FileChooser fc = new FileChooser();
            File newFile = fc.showSaveDialog(null);
            if ( newFile != null ) {
                // Check for a file extension and add ".txt" if missing
                if ( ! newFile.getName().contains(".") ) {
                    String newFilePath = newFile.getAbsolutePath();
                    newFilePath += ".txt";
                    newFile.delete();
                    newFile = new File(newFilePath);
                }
                file = newFile;
                openFileName = new String(newFile.getAbsolutePath());
                editor.filename = openFileName;
                selectedTab.setText(newFile.getName());
            }
        }
        else {
            // User is saving an existing file
            file = new File(openFileName);
        }

        // Write the content to the file
        try ( FileOutputStream fos = new FileOutputStream(file);
              BufferedOutputStream bos = new BufferedOutputStream(fos) ) {
            String text = editor.getText();
            bos.write(text.getBytes());
            bos.flush();
            success = true;
        }
        catch ( Exception e ) {
            success = false;
            System.out.println("File save failed (error: " + e.getLocalizedMessage() + ")");
            e.printStackTrace();
        }
        finally {
            if ( success ) {
                if ( editor != null ) {
                    editor.modified = false;
                }

                // The the tab's filename
                selectedTab.setText(file.getName());
            }
        }
    }
    
    private void handleKeyPress(KeyEvent ke) {
        boolean modifier = false;
        String text = ke.getText();
        KeyCode code = ke.getCode();
        if ( ke.isControlDown() || ke.isMetaDown() ) {
            modifier = true;
        }

        if ( modifier && text.equalsIgnoreCase("s") ) {
            saveFileRev();
            ignoreNextPress = true;
        }
        else if ( ! ignoreNextPress ) {
            if ( code == KeyCode.BACK_SPACE || 
                 code == KeyCode.ENTER ||
                 code == KeyCode.DELETE ) {
                indicateFileModified();
            }
            else if ( text != null && text.length() > 0 ) {
                if ( ! modifier ) {
                    indicateFileModified();
                }
            }
        }
    }
    
    public void stop() {
        // Go through all open files and save, then exit
        Iterator<Tab> iter = tabPane.getTabs().iterator();
        while ( iter.hasNext() ) {
            try {
                // Each file is saved by making each tab active then saving
                Tab tab = iter.next();
                Node node = tab.getContent();
                if ( node instanceof WebView ) {
                    TextArea area = (TextArea)node;
                    currentEditor = getEditorForTextArea(area);
                    if ( currentEditor.modified ) {
                        SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
                        selectionModel.select(tab);
                        saveFileRev();
                    }
                }
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
