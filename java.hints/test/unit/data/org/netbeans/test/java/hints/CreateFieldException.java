package org.netbeans.test.java.hints;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class CreateFieldException extends JLabel{
    
    public CreateFieldException() {
    }
    
    public Dimension getPreferredSize(JComponent c) {
        JComboBox jcb = (JComboBox) c;
        Object o = jcb.getPrototypeDisplayValue();
        Insets ins = ren.getInsets();
        
    }
    
}
