# FindIt
Java News Search Engine

Purpose:
Develop my own Search Engine. Have hands-on experience with the various modules which could be part of a search engine.

Modules Include:
1. Corpus pre-processing
2. User Interface
3. Dictionary building
4. Inverted Index Construction
5. Corpus Access
6. Boolean model of information retrieval
7. Vector Space Model of information retrieval
8. Wildcard management with additional bigram indexing
9. Bigram Language Model + Query Completion Module
10. Automatic thesaurus construction + Global Query Expansion with thesaurus (in VSM)
11. Text categorization using kNN + Topic Restriction

Ability:
The Search Engine now indexes and retrieves documents from Reuters-21578, which is a news collection, often used for text categorization and information retrieval. 

How To Run:
1. Open the project file in Eclipse.
2. Open the UserInterface.java class under “ui” package.
3. Run the project in Eclipse.

If it is the first time to run, the program will first handle all input files and generate required output files.
After the output files are ready, the program will be able to function.
I have included pre-generate files (JSON, processedJSON, Dictionary, WeightedIndex, BigramIndex, Thesaurus) in the /data/output folder, in order to save time.
Once all initializations are done, a GUI panel will show up and allow searching.

Task To Do:
Use better ways to handle output data to reduce generating time.
