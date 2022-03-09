package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.*;

public class AlphaRenars implements IBot{
    private static final int WIN_SCORE = 10;
    private static final int TIE_SCORE = 3;
    private static final int LOSS_SCORE = -10;
    final int moveTimeMs = 480;
    private String BOT_NAME = "Renars";

    Random rand = new Random();

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
    public IMove doMove(IGameState state) {
        return calculateWinningMove(state, moveTimeMs);
    }

    //Plays equal amount of random games for each available move, returns the one the provides most win %
    private IMove calculateWinningMove(IGameState state, int maxTimeMs){
        long startTime = System.currentTimeMillis();
        Node rootNode = new Node(state);
        rootNode.expand();//For each possible move from rootNode create a new child node
        List<Node> childrenList = rootNode.getChildren();
        while (System.currentTimeMillis() < startTime + maxTimeMs) { // check how much time has passed, stop if over maxTimeMs
            for (Node node : childrenList) {
                playOutRandomly_IncrementAccordingly(node,rootNode);
            }
        }
        Node bestNode = rootNode.getBestNode();
        IMove bestMove = bestNode.getMoveFromRoot();
        return bestMove;
    }

    private void playOutRandomly_IncrementAccordingly(Node node, Node rootNode) {
        GameSimulator gs = createSimulator(rootNode.getState());
        IMove randomMovePlayer = node.getMoveFromRoot();
        boolean win = false;

        while (gs.getGameOver()==GameOverState.Active){ // Game not ended
            List<IMove> moves = gs.getCurrentState().getField().getAvailableMoves();
            gs.updateGame(randomMovePlayer);
            win = true;

            // Opponent plays randomly
            if (gs.getGameOver()==GameOverState.Active){ // game still going
                moves = gs.getCurrentState().getField().getAvailableMoves();
                IMove randomMoveOpponent = moves.get(rand.nextInt(moves.size()));
                gs.updateGame(randomMoveOpponent);
                win = false;
            }
            if (gs.getGameOver()==GameOverState.Active){ // game still going
                moves = gs.getCurrentState().getField().getAvailableMoves();
                randomMovePlayer = moves.get(rand.nextInt(moves.size()));
            }
        }

        if (gs.getGameOver()==GameOverState.Win && win){
            node.addScore(WIN_SCORE);
        }
        if(gs.getGameOver()==GameOverState.Tie){
            node.addScore(TIE_SCORE);
        }
        if(gs.getGameOver()==GameOverState.Win && !win){
            node.addScore(LOSS_SCORE);
        }
        node.incrementVisits(1);
    }


    @Override
    public String getBotName() {
        return BOT_NAME;
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
            Move move = (Move) o;
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
        private List<Node> children;
        private double winScore = 0;
        private int visitCount = 0;
        private IGameState state;

        public IMove getMoveFromRoot() {
            return moveFromRoot;
        }

        public void setMoveFromRoot(IMove moveFromRoot) {
            this.moveFromRoot = moveFromRoot;
        }

        private IMove moveFromRoot;

        public Node(IGameState state){
            children = new ArrayList<>();
            this.state = state;

            String[][] board = Arrays.stream(state.getField().getBoard()).map(String[]::clone).toArray(String[][]::new);
            String[][] macroBoard = Arrays.stream(state.getField().getMacroboard()).map(String[]::clone).toArray(String[][]::new);
            this.state.getField().setBoard(board);
            this.state.getField().setMacroboard(macroBoard);
        }


        public IGameState getState() {
            return state;
        }

        public void setState(IGameState currentState) {
            this.state = currentState;
        }


        public void incrementVisits(int i) {
            visitCount+=i;
        }

        public void addScore(int winScore) {
            this.winScore+=winScore;
            /*if (parentNode!=null){
                parentNode.incrementVisits(1);
                parentNode.addScore(winScore);
            }

             */
        }

        public Node getBestNode() {
            return Collections.max(this.children, Comparator.comparing(c -> c.getWinProbability()));
        }

        private double getWinScore() {
            return winScore;
        }

        private double getWinProbability(){
            return ((double) (winScore / ((double) visitCount)));
        }

        private int getVisitCount() {
            return visitCount;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void expand() {
            IGameState currentState = this.state;
            List<IMove> availableMoves = this.getState().getField().getAvailableMoves();
            for (IMove availableMove : availableMoves) {
                Node node = new Node(state);
                children.add(node);
                node.setMoveFromRoot(availableMove);

                GameSimulator gs = createSimulator(currentState);
                gs.updateGame(availableMove);
                node.setState(gs.getCurrentState());
            }
        }

    }

    class Tree{
        private Node rootNode;

        public Tree(IGameState state){
            rootNode = new Node(state);
        }

        public Node getRootNode() {
            return rootNode;
        }
    }

}
