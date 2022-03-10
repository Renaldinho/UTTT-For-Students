package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.*;

public class MonteBot implements IBot {

    private static final int WIN_SCORE = 10;
    private final String botName = "Monte bot";
    private final int timeMs = 10;

    @Override
    public IMove doMove(IGameState state) {
        return getOptimalMove(state);
    }

    private IMove getOptimalMove(IGameState state) {
        long currentTime = System.currentTimeMillis();
        Tree tree = new Tree(state);//INIT Node tree;
        Node rootNode = tree.getRoot();//set root node

        int opponent = (state.getMoveNumber()+1)%2;//Get Opponent

        while (System.currentTimeMillis() < currentTime + timeMs){

            Node promisingNode = getPromisingNode(rootNode);
            GameSimulator gs = createSimulator(promisingNode.getState());

            if (gs.getGameOver()==GameOverState.Active)
                expandNode(promisingNode);

            if(promisingNode.getChildren().size()!=0){
                promisingNode = promisingNode.getRandomChild();
            }
            int resultPlayer = playoutRandomly(promisingNode,opponent);
            backPropagate(promisingNode,resultPlayer);

        }
        Node node = rootNode.getBestNode();
        return getMove(node,rootNode);
    }

    private IMove getMove(Node winner, Node root) {
        String[][] parentBoard = root.getState().getField().getBoard();
        String[][] childBoard = winner.getState().getField().getBoard();
        for(int i = 0; i < parentBoard.length; i++)
        {
            for(int j = 0; j < parentBoard[i].length; j++)
            {
                if(!parentBoard[i][j].equals(childBoard[i][j]))
                {
                    return new Move(i,j);
                }
            }
        }
        return null;
    }

    private void backPropagate(Node promisingNode, int resultPlayer) {
        Node tempNode = promisingNode;
        while(tempNode.getParent()!=null){
            tempNode.incrementVisits(1);
            if (tempNode.getState().getRoundNumber()+1%2== resultPlayer)
                tempNode.addScore(WIN_SCORE);
            tempNode = tempNode.getParent();
        }
    }

    private int playoutRandomly(Node promisingNode, int opponent) {
        Node tempNode = new Node(promisingNode.getState());
        GameSimulator gs = createSimulator(tempNode.getState());
        Random r = new Random();

        List<IMove> availableMoves;
        if(gs.getGameOver()!=GameOverState.Active && (gs.getCurrentState().getMoveNumber()+1)%2==opponent)
            tempNode.setWinScore(Integer.MIN_VALUE);//Playout of node was in favor of opponent so winscore is the min possible value
        while (gs.getGameOver()==GameOverState.Active){
            availableMoves = gs.getCurrentState().getField().getAvailableMoves();
            gs.updateGame(availableMoves.get(r.nextInt(availableMoves.size())));
        }
        int playerNo = (gs.getCurrentState().getMoveNumber()+1)%2;
        return  playerNo;


    }

    private void expandNode(Node promisingNode) {
        List<IMove> availableMoves = promisingNode.getState().getField().getAvailableMoves();
        for (IMove availableMove : availableMoves) {
            Node child = new Node(promisingNode.getState());
            promisingNode.getChildren().add(child);
            child.setParent(promisingNode);

            GameSimulator gs = createSimulator(child.getState());
            gs.updateGame(availableMove);
            child.setState(gs.getCurrentState());
        }
    }

    private Node getPromisingNode(Node rootNode) {
        Node node = rootNode;
        while (node.getChildren().size()!=0)
            node = UCB.getNodeWithHighestUCB(node);
        return node;
    }


    class Node{
        private IGameState gameState;
        private Node parent;
        private int visitCount;
        private double winScore;
        private List<Node> children;

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public int getVisitCount() {
            return visitCount;
        }

        public void setVisitCount(int visitCount) {
            this.visitCount = visitCount;
        }

        public double getWinScore() {
            return winScore;
        }

        public void setWinScore(double winScore) {
            this.winScore = winScore;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void setChildren(List<Node> children) {
            this.children = children;
        }

        public Node(IGameState gameState){
            this.gameState = new GameState();

            String[][] board = Arrays.stream(gameState.getField().getBoard()).map(String[]::clone).toArray(String[][]::new);
            String[][] macroBoard = Arrays.stream(gameState.getField().getMacroboard()).map(String[]::clone).toArray(String[][]::new);
            this.gameState.getField().setBoard(board);
            this.gameState.getField().setMacroboard(macroBoard);

            parent = null;
            visitCount = 0;
            winScore = 0;
            children = new ArrayList<>();
        }

        public IGameState getState() {
            return gameState;
        }

        public void setState(IGameState currentState) {
            gameState = currentState;
        }

        public Node getRandomChild() {
            Random r = new Random();
            return children.get(r.nextInt(children.size()));
        }

        public void incrementVisits(int i) {
            visitCount+=i;
        }

        public void addScore(int winScore) {
            this.winScore+=winScore;
        }

        public Node getBestNode() {
            return Collections.max(this.children, Comparator.comparing(c -> {
                return c.getVisitCount();
            }));
        }

    }

    class Tree{
        private Node rootNode;

        public Tree(IGameState state) {
            rootNode = new Node(state);
        }

        public Node getRoot() {
            return  rootNode;
        }
    }

    class UCB {

        public static Node getNodeWithHighestUCB(Node node) {
            List<Node> childrenList = node.getChildren();
            int parentVisits = node.getVisitCount();
            return Collections.max(childrenList, Comparator.comparing(child -> getUCBValue(parentVisits,child.getVisitCount(),child.getWinScore())));
        }
    }

    private static double getUCBValue(int parentVisits, int visitCount, double winScore) {
        if (visitCount == 0)
            return Integer.MAX_VALUE; //this node has not yet been visited meaning the upper confidence bound is infity.
        return ((Double) (winScore / visitCount)) // win ratio of node
                + 1.41 * Math.sqrt(Math.log(parentVisits) / visitCount);
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
        return botName;
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


}
