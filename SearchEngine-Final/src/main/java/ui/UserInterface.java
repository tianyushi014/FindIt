package ui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import base.Corpus;
import base.Dictionary;
import base.TextProcessor;
import index.BigramIndex;
import index.Thesaurus;
import index.WeightedIndex;
import query.QueryCompleter;
import query.QueryExpander;
import query.QueryProcessor;
import search.BooleanModel;
import search.VectorSpaceModel;
import util.DocObj;
import util.Utility;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import javax.swing.ListSelectionModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/*
 * Source Acknowledgement:
 * 
 * The layout of this panel is modified from
 * https://github.com/UnknownGi/Boolean-Retrieval-Model/blob/master/src/guiBooleanModel/GUI.java
 * 
 * The jList of topic selection is modified from
 * https://www.onlinetutorialspoint.com/java/java-jlist-multiple-selection-example.html
 * */


/**
 * Process:
 * 1. Initial search engine
 * 2. Load files, or create files if they do not exist
 * 3. Initial GUI
 * 4. Search
 * */
public class UserInterface extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private Utility util;
	
	public final int posCourses = 0;
	public final int posNews = 1;	
	
	private Corpus corpus;
	private Dictionary dictionary;
	private BigramIndex bigramIndex;
	private WeightedIndex weightedIndex;
	private Thesaurus thesaurus;
	
	private List<String> topics;
	private String[] topicsArr;
	private List<Map<Integer, DocObj>> documents;
	
	private QueryProcessor processorCourses;
	private QueryProcessor processorNews;
	
	private QueryCompleter completerCourses;
	private QueryCompleter completerNews;
	
	private QueryExpander expanderCourses;
	private QueryExpander expanderNews;
	
	private BooleanModel bmCourses;
	private BooleanModel bmNews;
	
	private VectorSpaceModel vsmCourses;
	private VectorSpaceModel vsmNews;
	
	
	private JLabel lblSE;
	private JLabel lblCorpus;
	private JLabel lblTopic;
	private JLabel lblExpand;
	
	private JComboBox<String> cbInput;
	
	private JRadioButton rdbtnCourses;
	private JRadioButton rdbtnNews;
	private JRadioButton rdbtnOthers;
	private ButtonGroup btnCorpus;
	
	private JList<String> lstAllTopics;
	private JList<Object> lstSelectedTopics;
	
	private JScrollPane scrollAll;
	private JScrollPane scrollSelected;
	
	private JButton btnSuggest;
	private JButton btnSelect;
	private JButton btnBoolean;
	private JButton btnVSM;
	
	private JCheckBox ckbExpand;
	
	
    
    public UserInterface() {
    	util = new Utility();
    	
    	topics = new ArrayList<String>();
    	documents = new ArrayList<Map<Integer, DocObj>>();
    	
    	try {
			initSE();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	initGUI();
	}
    
    
    /**
     * Initial search engine
     * @throws IOException
     * */
    private void initSE() throws IOException {	
    	//Courses Corpus
    	buildFiles(util.COURSES);
    	documents.add(loadDocuments(util.COURSES));
    	
    	//News Corpus
    	loadTopics();
    	buildFiles(util.REUTERS);
    	documents.add(loadDocuments(util.REUTERS));
    }
    
    
    private boolean checkFile(String fileName) {
    	File file = new File(fileName);   	
    	if (file.exists() && file.isFile())
    		return true;
    	return false;
    }
    
    /**
     * Load index files from local directory, or create files if does not exist
     * @throws IOException
     * */
    private void buildFiles(String selection) throws IOException {
    	String directory = util.outPath+selection;
    	
    	String json = directory+util.outJSON;
    	String processedJson = directory+util.outProcessedJSON;
    	String dic = directory+util.outDictionary;
    	String biIndex = directory+util.outBigramIndex;
    	String wIndex = directory+util.outWeightedIndex;
    	String tIndex = directory+util.outThesaurus;
    	
    	if (!checkFile(processedJson) || !checkFile(dic)) {
    		if (!checkFile(json)) {
				corpus = new Corpus(selection);
	    		corpus.build();
			}
    		
    		dictionary = new Dictionary(selection);
    		dictionary.build();
		}
    	
    	if (!checkFile(biIndex)) {
    		bigramIndex = new BigramIndex(selection);
    		bigramIndex.build();
		}

    	if (!checkFile(wIndex)) {
			weightedIndex = new WeightedIndex(selection);
			weightedIndex.build();
		}
    	
    	if (!checkFile(tIndex)) {
    		thesaurus = new Thesaurus(selection);
    		thesaurus.build();
		}
    }
    
    
    
    /**
	 * Read topics file and initial list
	 * @throws IOException 
	 * */
	private void loadTopics() throws IOException {
		String inFile = util.inPath+util.inTopics;
    	
		StringBuilder contentBuilder = new StringBuilder();
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		
		String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null) {
            contentBuilder.append(sCurrentLine).append("\n");
        }
        br.close();
        
        //String topicsStr = contentBuilder.toString();
		//topicsArr = contentBuilder.toString().split("\n");
		
		topics = Arrays.asList(contentBuilder.toString().split("\n"));
		
		TextProcessor analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);
		topics = analyzer.analyze(topics);
		
		topicsArr = new String[topics.size()];
		for (int i=0; i<topics.size(); i++)
			topicsArr[i] = topics.get(i);
		
		/*
		for (String topic : topics)
			System.out.print("["+topic+"] ");
		*/
        
        System.out.println("UI.loadTopics() Input: "+inFile);
        System.out.println("UI.loadTopics() Output-Size: "+topicsArr.length+"\n");
	}
	
	
	/**
	 * Read JSON file and initial Documents
	 * @throws IOException 
	 * */
	private Map<Integer, DocObj> loadDocuments(String selection) throws IOException {
		String directory = util.outPath+selection;
    	String inFile = directory+util.outJSON;
    	
    	Map<Integer, DocObj> documentMap = new HashMap<Integer, DocObj>();
		
		Gson gson = new GsonBuilder().create();
		
		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
		
		reader.beginArray();     
		while (reader.hasNext()) {
        	DocObj doc = gson.fromJson(reader, DocObj.class);
        	documentMap.put(doc.getID(), doc);
        }
        reader.close(); 
        
        System.out.println("UI.loadDocuments() Input: "+inFile);
        System.out.println("UI.loadDocuments() Output-Size: "+documentMap.size()+"\n");
        return documentMap;
	}	
	
	
	
	private void initGUI() {
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 400);		
		getContentPane().setLayout(null);
		
		setResizable(false);       
		setVisible(true);
		
		initComponents();
	}
	
	
	private void initComponents() {		
		lblSE = new JLabel("My Search Engine");
		lblSE.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
        lblSE.setHorizontalAlignment(SwingConstants.CENTER);
        
		lblCorpus = new JLabel("Please select from the following corpus: *");
		lblCorpus.setHorizontalAlignment(SwingConstants.CENTER);
        
		lblTopic = new JLabel("Additionally, you can select searching topic(s) for News corpus:");
		lblTopic.setHorizontalAlignment(SwingConstants.CENTER);
		
		lblExpand = new JLabel("Additionally, you can choose to expand query by thesaurus in VSM: ");
		lblExpand.setHorizontalAlignment(SwingConstants.CENTER);
		
		cbInput = new JComboBox<String>();
		cbInput.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		cbInput.setEditable(true);
		cbInput.setMaximumRowCount(11);
		
		lstAllTopics = new JList<String>(topicsArr);
		lstAllTopics.setFixedCellHeight(15);
		lstAllTopics.setFixedCellWidth(100);
		lstAllTopics.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstAllTopics.setVisibleRowCount(10);
        
        lstSelectedTopics = new JList<Object>();
		lstSelectedTopics.setFixedCellHeight(15);
		lstSelectedTopics.setFixedCellWidth(100);
		lstSelectedTopics.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstSelectedTopics.setVisibleRowCount(10);
        
        scrollAll = new JScrollPane(lstAllTopics);
        scrollSelected = new JScrollPane(lstSelectedTopics);
        
        btnSuggest = new JButton("Suggest");
		btnSuggest.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				btnSuggestMouseClicked(evt);
			}
		});
		
		btnSelect = new JButton("Select>>>"); 
		btnSelect.setEnabled(false);
		btnSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	lstSelectedTopics.setListData(lstAllTopics.getSelectedValuesList().toArray());
			}
        });
		
		btnBoolean = new JButton("Boolean Search");
		btnBoolean.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                btnBooleanMouseClicked(evt);
            }
        });
		
		btnVSM = new JButton("Vector Space Search");
		btnVSM.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                btnVSMMouseClicked(evt);
            }
        });
		
		ckbExpand = new JCheckBox("Expand Query");
		ckbExpand.setSelected(false);
		ckbExpand.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	        	if (ckbExpand.isSelected())
	        		btnBoolean.setEnabled(false);
	        	else
	        		btnBoolean.setEnabled(true);
	        }
	    });
        
		rdbtnCourses = new JRadioButton("Courses");
		rdbtnCourses.setSelected(true);
		rdbtnCourses.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	        	btnSelect.setEnabled(false);
	        }
	    });
		
		rdbtnNews = new JRadioButton("News");
		rdbtnNews.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	        	btnSelect.setEnabled(true);
	        }
	    });

		rdbtnOthers = new JRadioButton("Other");
		rdbtnOthers.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	        	btnSelect.setEnabled(false);
	        }
	    });
		
		btnCorpus = new ButtonGroup();
		btnCorpus.add(rdbtnCourses);
		btnCorpus.add(rdbtnNews);
		btnCorpus.add(rdbtnOthers);
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(20)
					.addComponent(lblExpand)
					.addGap(5)
					.addComponent(ckbExpand))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(110)
					.addComponent(btnBoolean)
					.addPreferredGap(ComponentPlacement.RELATED, 73, Short.MAX_VALUE)
					.addComponent(btnVSM)
					.addGap(110))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(240)
					.addComponent(lblSE, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(220, Short.MAX_VALUE))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(80)
					.addComponent(cbInput, GroupLayout.PREFERRED_SIZE, 400, GroupLayout.PREFERRED_SIZE)
					.addGap(10)
					.addComponent(btnSuggest)
					.addContainerGap(60, Short.MAX_VALUE))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(120)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(rdbtnCourses)
							.addGap(62)
							.addComponent(rdbtnNews)
							.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(rdbtnOthers))
						.addComponent(lblCorpus, GroupLayout.PREFERRED_SIZE, 360, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(120, Short.MAX_VALUE))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(120)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(lblTopic)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(scrollAll)
							.addComponent(btnSelect)
							.addComponent(scrollSelected)))
					.addContainerGap(120, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(40)
					.addComponent(lblSE)
					.addGap(40)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(cbInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSuggest))
					//.addComponent(cbInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(40)
					.addComponent(lblCorpus)
					.addGap(15)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(rdbtnCourses)
						.addComponent(rdbtnOthers)
						.addComponent(rdbtnNews))
					.addGap(20)
					.addComponent(lblTopic)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(scrollAll)
						.addComponent(btnSelect)
						.addComponent(scrollSelected))
					.addGap(20)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblExpand)
						.addComponent(ckbExpand))
					.addGap(30)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnBoolean)
						.addComponent(btnVSM))
					.addContainerGap(40, Short.MAX_VALUE))
		);
        getContentPane().setLayout(groupLayout);
        
        pack();
	}
	
	
	
	public void btnSuggestMouseClicked(MouseEvent evt) {
		String input = (String) cbInput.getSelectedItem();
		String[] suggestions;
		
		if (rdbtnCourses.isSelected()) {
			processorCourses = new QueryProcessor(util.COURSES);
	    	completerCourses = new QueryCompleter(util.COURSES, processorCourses);
			suggestions = completerCourses.suggest(input);
		} else {
			processorNews = new QueryProcessor(util.REUTERS);
	    	completerNews = new QueryCompleter(util.REUTERS, processorNews);
	    	suggestions = completerNews.suggest(input);
		}
		
		String[] results = new String[suggestions.length+1];
		
		results[0] = input;
		for (int i=1; i<results.length; i++) {
			results[i] = input+" "+suggestions[i-1];
		}
		
		cbInput.setModel(new DefaultComboBoxModel<String>(results));
		cbInput.showPopup();
	}
	
	
	
	public void btnBooleanMouseClicked(MouseEvent evt) {
		String query = (String) cbInput.getSelectedItem();
		
		List<String> selectedTopics = new ArrayList<String>();

		try {
			if (rdbtnCourses.isSelected()) {
				processorCourses = new QueryProcessor(util.COURSES);
		    	bmCourses = new BooleanModel(util.COURSES);
		    	
				bmSearch(query, posCourses, processorCourses, bmCourses, selectedTopics);
			} else if (rdbtnNews.isSelected()) {
				processorNews = new QueryProcessor(util.REUTERS);
		    	bmNews = new BooleanModel(util.REUTERS);
		    	
				for (int i = 0; i < lstSelectedTopics.getModel().getSize(); i++) {
		            Object item = lstSelectedTopics.getModel().getElementAt(i);
		            selectedTopics.add(Objects.toString(item, null));
		        }
				
				bmSearch(query, posNews, processorNews, bmNews, selectedTopics);
			} else if (rdbtnOthers.isSelected()) {
				JOptionPane.showMessageDialog(null, "Collection Not Supported Yet!");
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Clear input field
		cbInput.removeAllItems();
		
		//Clear topic selections
		lstAllTopics.clearSelection();
		lstSelectedTopics.setListData(new Object[] {});
	}
	
	
	
	
	private void bmSearch(String query, int pos, QueryProcessor processor, BooleanModel bm, List<String> selectedTopics) throws IOException {
		System.out.println("\nUI.bmSearch() QP: "+ processor.getInFile());
		System.out.println("UI.bmSearch() BM: "+ bm.getInFile());
		
		//Query Pre-processing
		List<String> queryStream = processor.booleanQuery(query);
		
		//All search results (docIDs), without restriction of topic selections
		List<Integer> docIDs = bm.search(queryStream);
		
		if (docIDs.size()>0) {
			//Final search results (DocObj), with restriction of topic selections
			Map<Integer, DocObj> resultDocs = docsSubset(documents.get(pos), docIDs);
			List<Integer> resultIDs = docIDs;
			
			if (selectedTopics.size() > 0) {
				resultDocs = topicFilter(resultDocs, selectedTopics);
				resultIDs.clear();
				resultIDs.addAll(resultDocs.keySet());
			}
			
			if (resultIDs.size()>0) {
				System.out.println("UI.bmSearch() Input: "+query);	
				System.out.println("UI.bmSearch() Query Tokens: "+queryStream);	
				System.out.println("UI.bmSearch() SelectedTopics: "+selectedTopics);
				System.out.println("UI.bmSearch() Output: "+resultIDs);
				System.out.println("UI.bmSearch() Output-Size: "+resultIDs.size()+"\n");
				
				ResultsPanel rtp = new ResultsPanel(resultDocs, resultIDs);
	            rtp.setVisible(true);
	            
	            JOptionPane.showMessageDialog(null, "Boolean Model: Found "+resultDocs.size()+" Matched Documents!");
			} else 
				JOptionPane.showMessageDialog(null, "No Results Exist For Such Query!");
		} else
        	JOptionPane.showMessageDialog(null, "No Results Exist For Such Query!");
		
		//return docIDs;
	}
	
	
	
	public void btnVSMMouseClicked(MouseEvent evt) {
		String query = (String) cbInput.getSelectedItem();
		
		boolean toExpand = false;
		if (ckbExpand.isSelected())
			toExpand = true;
		
		List<String> selectedTopics = new ArrayList<String>();
		
		try {
			if (rdbtnCourses.isSelected()) {
				processorCourses = new QueryProcessor(util.COURSES);
		    	expanderCourses = new QueryExpander(util.COURSES);
		    	vsmCourses = new VectorSpaceModel(util.COURSES);
		    	
				vsmSearch(query, posCourses, processorCourses, expanderCourses, 
						toExpand, vsmCourses, selectedTopics);
			
			} else if (rdbtnNews.isSelected()) {
				processorNews = new QueryProcessor(util.REUTERS);
		    	expanderNews = new QueryExpander(util.REUTERS);
		    	vsmNews = new VectorSpaceModel(util.REUTERS);
		    	
		    	for (int i = 0; i < lstSelectedTopics.getModel().getSize(); i++) {
		            Object item = lstSelectedTopics.getModel().getElementAt(i);
		            selectedTopics.add(Objects.toString(item, null));
		        }
				
				vsmSearch(query, posNews, processorNews, expanderNews, 
						toExpand, vsmNews, selectedTopics);
			
			} else if (rdbtnOthers.isSelected()) {
				JOptionPane.showMessageDialog(null, "Collection Not Supported Yet!");
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//txtInput.setText("");
		cbInput.removeAllItems();
		
		//Clear topic selections
		lstAllTopics.clearSelection();
		lstSelectedTopics.setListData(new Object[] {});
	}
	
	
	private void vsmSearch(String query, int pos, QueryProcessor processor, QueryExpander expander, boolean toExpand,
			VectorSpaceModel vsm, List<String> selectedTopics) throws IOException {
		System.out.println("UI.vsmSearch() QP: "+ processor.getInFile());
		System.out.println("UI.vsmSearch() QE: "+ expander.getInFile());
		System.out.println("UI.vsmSearch() VSM: "+ vsm.getInFile()+"\n");
		
		//Query Pre-processing
		List<String> queryStream = processor.vsmQuery(query);
		
		//Query Expansion
		if (toExpand) {
			//if (pos == posCourses) {
				queryStream = expander.expand(queryStream);
			//}
		}
		
		//All search results, without restriction of topic selections
		Map<Integer, Double> docIDsScores = vsm.search(queryStream);
		//System.out.println("UI.vsmSearch() AllTopics: "+docIDsScores);
		
		List<Integer> docIDs = new ArrayList<Integer>();
		docIDs.addAll(docIDsScores.keySet());
		
		if (docIDsScores.size()>0) {
			//Final search results, with restriction of topic selections
			Map<Integer, DocObj> resultDocs = docsSubset(documents.get(pos), docIDs);
			Map<Integer, Double> resultIDsScores = new HashMap<Integer, Double>();
			resultIDsScores.putAll(docIDsScores);
			
			if (selectedTopics.size() > 0) {
				resultDocs = topicFilter(resultDocs, selectedTopics);
				resultIDsScores.clear();
				for (int docID : resultDocs.keySet()) {
					resultIDsScores.put(docID, docIDsScores.get(docID));
				}
			}
			
			if (resultIDsScores.size()>0) {
				resultIDsScores = sortResults(resultIDsScores);
				
				System.out.println("\nUI.vsmSearch() Input: "+query);
				System.out.println("UI.vsmSearch() Query Tokens: "+queryStream);	
				System.out.println("UI.vsmSearch() SelectedTopics: "+selectedTopics);
				System.out.println("UI.vsmSearch() AllTopics: "+docIDsScores);
				System.out.println("UI.vsmSearch() Output: "+resultIDsScores);
				System.out.println("UI.vsmSearch() Output-Size: "+resultDocs.size()+"\n");	
				
				ResultsPanel rtp = new ResultsPanel(resultDocs, resultIDsScores);
	            rtp.setVisible(true);
	            
	            if (toExpand)
	            	JOptionPane.showMessageDialog(null, "Vector Space Model: Found "+resultDocs.size()+" Documents for Query "+queryStream+".");
	            else
	            	JOptionPane.showMessageDialog(null, "Vector Space Model: Found "+resultDocs.size()+" Matched Documents!");
			} else
				JOptionPane.showMessageDialog(null, "No Results Exist For Such Query!");
        } else
        	JOptionPane.showMessageDialog(null, "No Results Exist For Such Query!");
	}
	
	
	
	private Map<Integer, DocObj> docsSubset(Map<Integer, DocObj> docsSet, List<Integer> resultIDs) {
		Map<Integer, DocObj> subset = new HashMap<Integer, DocObj>();		
		for (Integer docID : resultIDs) {
			DocObj doc = docsSet.get(docID);
			subset.put(docID, doc);
		}
		return subset;
	}
	
	
	private Map<Integer, DocObj> topicFilter(Map<Integer, DocObj> docsSet, List<String> selectedTopics) {
		Map<Integer, DocObj> subset = new HashMap<Integer, DocObj>();
		
		for (int docID : docsSet.keySet()) {
			DocObj doc = docsSet.get(docID);
			
			for (String topic : selectedTopics) {
				if (doc.getTopic().contains(topic))
					subset.put(docID, doc);
			}
		}
		
		return subset;
	}
	
	
	private Map<Integer, Double> sortResults(Map<Integer, Double> idsScores) {
		List<Map.Entry<Integer, Double> > list = new LinkedList<Map.Entry<Integer, Double>>(idsScores.entrySet());
        
        //Sort list (results are in ascending order)
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() { 
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
        
        //Revise scores map and results list
        Map<Integer, Double> sortedScores = new LinkedHashMap<Integer, Double>();
        ArrayList<Integer> sortedResults = new ArrayList<Integer>();
        
        for (int i=list.size(); i>0; i--) {
        	Map.Entry<Integer, Double> tmp = list.get(i-1);
        	sortedScores.put(tmp.getKey(), tmp.getValue());
        	sortedResults.add(tmp.getKey());
        }
        
        return sortedScores;
	}
	
	
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UserInterface ui = new UserInterface();
					ui.setVisible(true);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});		
	}
}