package ui;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import util.DocObj;

import javax.swing.GroupLayout.Alignment;


/*
 * Source Acknowledgement:
 * 
 * The layout of this panel is modified from
 * https://github.com/UnknownGi/Boolean-Retrieval-Model/blob/master/src/guiBooleanModel/ResultTextPanel.java
 * 
 * */


public class ResultsPanel extends JFrame {

	private static final long serialVersionUID = 1L;
	
	
	//Map: key = docID, value = document
	private Map<Integer, DocObj> docs;
	
	private Map<Integer, Double> results;
	private List<Integer> docIDs;
	private List<Double> scores;
	
	private List<String> topics;
	private List<String> titles;
	private List<String> contents;
	private List<String> excerpts;
	private String[] displays;
	
	
	private JLabel lblSE;
    private JComboBox<String> cbResults;
    private JButton btnCancel;
    private JButton btnOpen;
    
    
    /**
     * @wbp.parser.constructor
     */
    public ResultsPanel(Map<Integer, DocObj> documentMap, List<Integer> docIDs) {
    	docs = documentMap;	
    	this.docIDs = docIDs;
    	
    	topics = new ArrayList<String>();
    	titles = new ArrayList<String>();
		contents = new ArrayList<String>();
		excerpts = new ArrayList<String>();
		displays = new String[docs.size()];
    	
    	init();
    	initGUI();
    }
    
    public ResultsPanel(Map<Integer, DocObj> documentMap, Map<Integer, Double> results) {
    	docs = documentMap;
    	this.results = results;
    	
    	docIDs = new ArrayList<Integer>();
    	scores = new ArrayList<Double>();   	
    	
    	topics = new ArrayList<String>();
    	titles = new ArrayList<String>();
		contents = new ArrayList<String>();
		excerpts = new ArrayList<String>();
		displays = new String[docs.size()];
    	
    	init();
    	initGUI();
    }
    
    
    private void init() {
    	if (results!=null) {
        	for (Entry<Integer, Double> result : results.entrySet()) {
        		docIDs.add(result.getKey());
        		scores.add(result.getValue());
        	}
        }
    	
    	for (Integer docID : docIDs) {
    		DocObj doc = docs.get(docID);
    		
    		//System.out.println("ResultsPanel.init() Document: "+doc);
    		
    		topics.add(doc.getTopic());
    		titles.add(doc.getTitle());
    		
    		String content = doc.getContent();
    		contents.add(content);
    		
    		if (content.length()>0) {
				excerpts.add(content.split("\\.")[0]+".");
			} else {
				excerpts.add("");
			}
    	}
    	
    	//Only vector space model shows scores
    	if (results!=null) {
        	for (int i=0; i<results.size(); i++) {
        		displays[i] = ("[Score "+scores.get(i)+"] "+titles.get(i)+": "+excerpts.get(i));
        	}
        } else {
        	for (int i=0; i<docIDs.size(); i++) {
        		displays[i] = (titles.get(i)+": "+excerpts.get(i));
        	}
        }
    }
	
	
	
    
    private void initGUI() {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 450, 300);
		getContentPane().setLayout(null);
		
		setResizable(false);
		setVisible(true);
		
		initComponents();
	}
	
	
	
    private void initComponents() {
		lblSE = new JLabel("Search Results");
		lblSE.setSize(120, 20);
		lblSE.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
        
		cbResults = new JComboBox<String>();
		cbResults.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		cbResults.setLocation(25, 65);
		cbResults.setModel(new DefaultComboBoxModel<String>(displays));
		cbResults.setMaximumRowCount(10);

        
        btnCancel = new JButton("Cancel");
        btnCancel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                btnCancelMouseClicked(evt);
            }
        });

        btnOpen = new JButton("Open");
        btnOpen.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                btnOpenMouseClicked(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        layout.setHorizontalGroup(
        	layout.createParallelGroup(Alignment.LEADING)
        		.addGroup(layout.createSequentialGroup()
        			.addGroup(layout.createParallelGroup(Alignment.LEADING)
        				.addGroup(layout.createSequentialGroup()
        					.addGap(128)
        					.addComponent(btnOpen)
        					.addGap(53)
        					.addComponent(btnCancel))
        				.addGroup(layout.createSequentialGroup()
        					.addGap(26)
        					.addComponent(cbResults, GroupLayout.PREFERRED_SIZE, 413, GroupLayout.PREFERRED_SIZE))
        				.addGroup(layout.createSequentialGroup()
        					.addGap(179)
        					.addComponent(lblSE)))
        			.addContainerGap(36, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
        	layout.createParallelGroup(Alignment.TRAILING)
        		.addGroup(layout.createSequentialGroup()
        			.addContainerGap(27, Short.MAX_VALUE)
        			.addComponent(lblSE)
        			.addGap(18)
        			.addComponent(cbResults, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
        			.addGap(20)
        			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(btnCancel)
        				.addComponent(btnOpen))
        			.addGap(25))
        );
        getContentPane().setLayout(layout);
        
        pack();
	}
	
	
	public void btnCancelMouseClicked(MouseEvent evt) {
		this.setVisible(false);
	}
	
	public void btnOpenMouseClicked(MouseEvent evt) {
		int index = cbResults.getSelectedIndex();
	
		String details = "";
		
		if (!topics.get(index).equals(""))
			details = details+"Topic: "+topics.get(index)+"\n\n";
		
		details = details+"Title: "+titles.get(index)+"\n\n"+"Content: "+contents.get(index);
		
		DetailsPanel dp = new DetailsPanel(details);
		dp.setVisible(true);
	}
}
