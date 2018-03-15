 

import javafx.scene.control.TextArea;

/**
 *
 * @author ericjbruno
 */
public class SimpleEditor implements Content {
    public boolean modified = false;
    public TextArea textArea = new TextArea();
    public String filename = null;

    public boolean isModified() {
        return modified;
    }
    
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public TextArea getRoot() {
        return textArea;
    }

    public void setText(String text) {
        textArea.setText(text);
    }
    
    public String getText() {
        return textArea.getText();
    }
}
