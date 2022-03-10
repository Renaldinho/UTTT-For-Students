package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.*;

public class MegaTrashBot implements IBot{

    private final int timeMs = 500;
    private static final String BOTNAME="Mega trash bot";


    @Override
    public IMove doMove(IGameState state) {
        return calculateOptimalMove(state,timeMs);
    }

    // Plays single games until it wins and returns the first move for that. If iterations reached with no clear win, just return random valid move
    private IMove calculateOptimalMove(IGameState state, int maxTimeMs){
        Tree tree = new Tree(state);
        Node rootNode = tree.getRootNode();

        long time = System.currentTimeMillis();
        Random rand = new Random();
        while (System.currentTimeMillis() < time + maxTimeMs) {
            Node promisingNode = selectPromisingNode(rootNode);
            GameSimulator gs = createSimulator(promisingNode.getGameState());
            if (gs.getGameOver() == GameOverState.Active) {
                expandNode(promisingNode);
            }
            Node nodeToExplore = promisingNode;
            if (promisingNode.getChildList().size() > 0) {
                nodeToExplore = promisingNode.getRandomChildNode();
            }
            String playoutResult = simulateRandomPlayout(nodeToExplore);
            backPropogate(nodeToExplore, playoutResult);
        }



            //printChildBoards(currentNode);
        List<IMove> availableMoves = rootNode.getGameState().getField().getAvailableMoves();
        return availableMoves.get(rand.nextInt(availableMoves.size()));
    }

    private void backPropogate(Node nodeToExplore, String playoutWinner) {
        Node tempNode = nodeToExplore;
        while (tempNode!=null){
            tempNode.incrementVisits(1);
            String player = tempNode.getGameState().getMoveNumber()%2==0 ? "1" : "0";
            if (player.equals(playoutWinner))
                tempNode.addWinScore(10);

            tempNode = tempNode.getParent();
        }
    }

    private String simulateRandomPlayout(Node nodeToExplore) {
        GameSimulator simulator = createSimulator(nodeToExplore.getGameState());
        String player = nodeToExplore.getGameState().getMoveNumber()%2==0 ? "1" : "0";
        Node tempNode = nodeToExplore;
        Random random  = new Random();

        List<IMove> availableMoves;
        while (simulator.getGameOver()==GameOverState.Active){

            availableMoves = simulator.getCurrentState().getField().getAvailableMoves();
            simulator.updateGame(availableMoves.get(random.nextInt(availableMoves.size())));
            if (simulator.getGameOver()==GameOverState.Win || simulator.getGameOver()==GameOverState.Tie )
                return player;


            availableMoves = simulator.getCurrentState().getField().getAvailableMoves();
            simulator.updateGame(availableMoves.get(random.nextInt(availableMoves.size())));
            if (simulator.getGameOver()==GameOverState.Win || simulator.getGameOver()==GameOverState.Tie )
                return player=="1" ? "0" : "1";
        }
        return "-1";
    }

    private void expandNode(Node promisingNode) {
        List<IMove> availableMoves = promisingNode.getGameState().getField().getAvailableMoves();
        availableMoves.forEach(move -> {
            GameSimulator gs = createSimulator(promisingNode.getGameState());
            gs.updateGame(move);
            Node childNode = new Node(gs.getCurrentState());
            promisingNode.getChildList().add(childNode);
            childNode.setParent(promisingNode);
        });
    }

    private Node selectPromisingNode(Node rootNode) {
        Node node = rootNode;
        if (!node.visited)
            return node;
        while (rootNode.getChildList().size()>0)
            node = UCB.getChildWithHighestUCB(node);
        return node;
    }

    private void playUntilTerminal(Node node) {
        Random rand = new Random();
        GameSimulator gs = createSimulator(node.getGameState());
        List<IMove> moves;
        while (gs.getGameOver()==GameOverState.Active){
                moves = gs.getCurrentState().getField().getAvailableMoves();

                IMove randomMovePlayer = moves.get(rand.nextInt(moves.size()));
                gs.updateGame(randomMovePlayer);

            if (gs.getGameOver()==GameOverState.Win){
                printBoard(gs.getCurrentState().getField().getBoard());
                System.out.println("win");
            }


            if (gs.getGameOver()==GameOverState.Active){ // game still going
                moves = gs.getCurrentState().getField().getAvailableMoves();
                IMove randomMoveOpponent = moves.get(rand.nextInt(moves.size()));
                gs.updateGame(randomMoveOpponent);
            }
        }
    }

    private void printChildBoards(Node currentNode) {
        for(Node node: currentNode.getChildList()){
            String[][] board = node.getGameState().getField().getBoard();
            for (int x = 0; x<board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    System.out.print(board[x][y]+" ");
                }
                System.out.println("");
            }
            System.out.println("__________________");
        }
    }

    private void printBoard(String[][] board){
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                System.out.print(board[x][y]+" ");
            }
            System.out.println(" ");
        }
        System.out.println("______________________");
    }

    private GameSimulator createSimulator(IGameState state) {
        GameSimulator simulator = new GameSimulator(new GameState());
        simulator.setGameOver(GameOverState.Active);
        simulator.setCurrentPlayer(state.getMoveNumber() % 2);
        simulator.getCurrentState().setRoundNumber(state.getRoundNumber());
        simulator.getCurrentState().setMoveNumber(state.getMoveNumber());
        simulator.getCurrentState().getField().setBoard(state.getField().getBoard());
        simulator.getCurrentState().getField().setMacroboard(state.getField().getMacroboard());
        return simulator;
    }

    @Override
    public String getBotName() {
        return BOTNAME;
    }

    public enum GameOverState {
        Active,
        Win,
        Tie
    }

    public class Move implements IMove {
        int x = 0;
        int y = 0;

        public Move(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MachinaBot.Move move = (MachinaBot.Move) o;
            return x == move.x && y == move.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    class GameSimulator {
        private final IGameState currentState;
        private int currentPlayer = 0; //player0 == 0 && player1 == 1
        private volatile GameOverState gameOver = GameOverState.Active;

        public void setGameOver(GameOverState state) {
            gameOver = state;
        }

        public GameOverState getGameOver() {
            return gameOver;
        }

        public void setCurrentPlayer(int player) {
            currentPlayer = player;
        }

        public IGameState getCurrentState() {
            return currentState;
        }

        public GameSimulator(IGameState currentState) {
            this.currentState = currentState;
        }

        public Boolean updateGame(IMove move) {
            if (!verifyMoveLegality(move))
                return false;

            updateBoard(move);
            currentPlayer = (currentPlayer + 1) % 2;

            return true;
        }

        private Boolean verifyMoveLegality(IMove move) {
            IField field = currentState.getField();
            boolean isValid = field.isInActiveMicroboard(move.getX(), move.getY());

            if (isValid && (move.getX() < 0 || 9 <= move.getX())) isValid = false;
            if (isValid && (move.getY() < 0 || 9 <= move.getY())) isValid = false;

            if (isValid && !field.getBoard()[move.getX()][move.getY()].equals(IField.EMPTY_FIELD))
                isValid = false;

            return isValid;
        }

        private void updateBoard(IMove move) {
            String[][] board = currentState.getField().getBoard();
            board[move.getX()][move.getY()] = currentPlayer + "";
            currentState.setMoveNumber(currentState.getMoveNumber() + 1);
            if (currentState.getMoveNumber() % 2 == 0) {
                currentState.setRoundNumber(currentState.getRoundNumber() + 1);
            }
            checkAndUpdateIfWin(move);
            updateMacroboard(move);

        }

        private void checkAndUpdateIfWin(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            int macroX = move.getX() / 3;
            int macroY = move.getY() / 3;

            if (macroBoard[macroX][macroY].equals(IField.EMPTY_FIELD) ||
                    macroBoard[macroX][macroY].equals(IField.AVAILABLE_FIELD)) {

                String[][] board = getCurrentState().getField().getBoard();

                if (isWin(board, move, "" + currentPlayer))
                    macroBoard[macroX][macroY] = currentPlayer + "";
                else if (isTie(board, move))
                    macroBoard[macroX][macroY] = "TIE";

                //Check macro win
                if (isWin(macroBoard, new Move(macroX, macroY), "" + currentPlayer))
                    gameOver = GameOverState.Win;
                else if (isTie(macroBoard, new Move(macroX, macroY)))
                    gameOver = GameOverState.Tie;
            }

        }

        private boolean isTie(String[][] board, IMove move) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            for (int i = startX; i < startX + 3; i++) {
                for (int k = startY; k < startY + 3; k++) {
                    if (board[i][k].equals(IField.AVAILABLE_FIELD) ||
                            board[i][k].equals(IField.EMPTY_FIELD))
                        return false;
                }
            }
            return true;
        }


        public boolean isWin(String[][] board, IMove move, String currentPlayer) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            //check col
            for (int i = startY; i < startY + 3; i++) {
                if (!board[move.getX()][i].equals(currentPlayer))
                    break;
                if (i == startY + 3 - 1) return true;
            }

            //check row
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][move.getY()].equals(currentPlayer))
                    break;
                if (i == startX + 3 - 1) return true;
            }

            //check diagonal
            if (localX == localY) {
                //we're on a diagonal
                int y = startY;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][y++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }

            //check anti diagonal
            if (localX + localY == 3 - 1) {
                int less = 0;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][(startY + 2) - less++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }
            return false;
        }

        private void updateMacroboard(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            for (int i = 0; i < macroBoard.length; i++)
                for (int k = 0; k < macroBoard[i].length; k++) {
                    if (macroBoard[i][k].equals(IField.AVAILABLE_FIELD))
                        macroBoard[i][k] = IField.EMPTY_FIELD;
                }

            int xTrans = move.getX() % 3;
            int yTrans = move.getY() % 3;

            if (macroBoard[xTrans][yTrans].equals(IField.EMPTY_FIELD))
                macroBoard[xTrans][yTrans] = IField.AVAILABLE_FIELD;
            else {
                // Field is already won, set all fields not won to avail.
                for (int i = 0; i < macroBoard.length; i++)
                    for (int k = 0; k < macroBoard[i].length; k++) {
                        if (macroBoard[i][k].equals(IField.EMPTY_FIELD))
                            macroBoard[i][k] = IField.AVAILABLE_FIELD;
                    }
            }
        }
    }

    class Node{
        private Node parent;
        private List<Node> childList;
        private IGameState state;
        private int visitCount = 0;
        double winScore = 0;
        boolean visited = false;

        public Node(IGameState state) {
            this.state = state;
            childList = new ArrayList<>();
        }

        public void setVisitCount(int visitCount) {
            this.visitCount = visitCount;
        }

        public void incrementVisits(int i){
            visitCount +=i;
        }

        public int getVisitCount() {
            return visitCount;
        }


        public List<Node> getChildList() {
            return childList;
        }

        public void setGameState(IGameState state){
            this.state = state;
        }

        public IGameState getGameState(){
            return state;
        }

        public void setVisitedTrue(){
            visited = true;
        }

        public double getWinscore() {
            return winScore;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public Node getRandomChildNode() {
            Random random = new Random();
            return childList.get(random.nextInt(childList.size()));
        }

        public Node getParent() {
            return parent;
        }

        public void addWinScore(int i) {
            winScore += i;
        }
    }


    class Tree{
        private Node rootNode;

        public Tree(IGameState state){
            rootNode = new Node(state);
        }

        public Node getRootNode(){
            return rootNode;
        }
    }

    class UCB{

        private static double getUCBValue(int parentVisits,int nodeVisits,double nodeWinScore){
            if (nodeVisits == 0)
                return Integer.MAX_VALUE; //this node has not yet been visited meaning the upper confidence bound is infity.
            return ((Double) (nodeWinScore / nodeVisits)) // win ratio of node
                    + 1.41 * Math.sqrt(Math.log(parentVisits) / nodeVisits);
        }

        public static Node getChildWithHighestUCB(Node node){
            int parentVisits = node.getVisitCount();
            List<Node> children = node.getChildList();
            /*double highestUCB = Integer.MIN_VALUE;
            Node bestNode = children.get(0);
            for(Node child: children){
                double ucbValue = getUCBValue(parentVisits,child.getVisitCount(),child.winScore);
                if (ucbValue>highestUCB){
                    highestUCB = ucbValue;
                    bestNode = child;
                }
            }
            return bestNode;

             */
            return Collections.max(children,Comparator.comparingDouble(childNode -> getUCBValue(parentVisits, childNode.getVisitCount(),childNode.winScore)));


        }
    }
}
