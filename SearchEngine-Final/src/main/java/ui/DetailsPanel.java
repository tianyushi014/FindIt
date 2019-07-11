package ui;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.WindowConstants;


/*
 * Source Acknowledgement:
 * 
 * The layout of this panel is modified from
 * https://github.com/UnknownGi/Boolean-Retrieval-Model/blob/master/src/guiBooleanModel/DocumentContent.java
 * 
 * */


public class DetailsPanel extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private JLabel jLabel1;
    private JTextArea jTextArea1;
    private JScrollPane jScrollPane1;
    private JButton jButton1;    

    private String details;
	
    
	public DetailsPanel(String details) {
		this.details = details;
		initGUI();		
	}

	
	private void initGUI() {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);		
		getContentPane().setLayout(null);
		
		setResizable(false);
		setVisible(true);
		
		initComponnent();
	}
	
	
	private void initComponnent() {		
		jLabel1 = new JLabel("Document Content");
		jLabel1.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		
		jTextArea1 = new JTextArea(details);
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setFocusable(false);
        jTextArea1.setFont(new Font("Lucida Grande", 0, 16)); // NOI18N
               
        jScrollPane1 = new JScrollPane();
		jScrollPane1.setViewportView(jTextArea1);
		
        jButton1 = new JButton("Close");
        jButton1.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });
             
        GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            	.addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addContainerGap())
                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton1)
                        .addGap(212, 212, 212))))
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(172, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(160))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20)
                .addComponent(jLabel1)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(jButton1)
                .addGap(20))
        );
        getContentPane().setLayout(layout);
        
        pack();
	}
	
	
	private void jButton1MouseClicked(MouseEvent evt) {
		this.setVisible(false);
	}
}
