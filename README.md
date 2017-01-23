# xword

In Daniel Dennet’s book “Intuition Pumps” (https://books.google.ca/books?id=sicVcPjfPxUC) he uses the double crossword as an example of a highly constrained problem which are difficult to generate. That is, a crossword puzzle which has two entirely unique solutions. I took this as a personal challenge and wrote three puzzle generating variants, which can be found on the 3 branches in this repository: 

1. A grid-like crossword:
![grid-0](https://cloud.githubusercontent.com/assets/5944064/22226454/8e0d286a-e194-11e6-86c7-2ec923e8ed51.png)
![grid-1](https://cloud.githubusercontent.com/assets/5944064/22226453/8e0aac2a-e194-11e6-8c30-0cb6886a8028.png)


2. Two parallel words as a stem:
![parallel-0](https://cloud.githubusercontent.com/assets/5944064/22226458/8e12c2ca-e194-11e6-9968-60a106283e98.png)
![parallel-1](https://cloud.githubusercontent.com/assets/5944064/22226456/8e115f48-e194-11e6-9d1e-9ee613b06d70.png)


3. A square box as a stem:
![square-0](https://cloud.githubusercontent.com/assets/5944064/22226457/8e117820-e194-11e6-976e-8e687eacc6ec.png)
![square-1](https://cloud.githubusercontent.com/assets/5944064/22226455/8e0f0086-e194-11e6-87c2-60d59004648d.png)

An interactive sample puzzle can be solved at the following URLs:<br/>
[Puzzle 0](http://crosswordcomposer.com/?author=Jamie+Hlusko&title=0-Double-Crossword-1452827001720&solve=true)<br/>
[Puzzle 1](http://crosswordcomposer.com/?author=Jamie+Hlusko&title=1-Double-Crossword-1452827001720&solve=true)

Note: I am not affiliated with http://crosswordcomposer.com. This repository will just create the raw XML files that a reader like http://crosswordcomposer.com can display.

For each of the branches/puzzle types the default method is to create whole puzzles which the user can then review and either reject causing the program to generate a new puzzle, or export the puzzle to XML.

The biggest limitation is the clue database. I used Wordnet (http://wordnet.princeton.edu/) to find semantically related equal length words as a basis for clues. The database does not distinguish between obscure, obscene or technical jargon including abbreviations. The clues also often use the answer as part of the clue.

For this reason I created the additional features (at least in the main grid branch), where a user can build a puzzle up word by word - rejecting inappropriate words as they go. At the end of each session the database state is preserved, so eventually the user will refine the database to their liking. A quicker solution is to filter out the least common words. I used the word frequency list from http://wordcount.org. For clues which contain the solution, the clues must be determined by a human, either by manually editing the XPF file, or using the UI provided by a site like http://crosswordcomposer.com.

I hope you enjoy creating and solving your own double crosswords!
