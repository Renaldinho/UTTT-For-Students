package dk.easv.bll.bot;

import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class HarryBotter implements IBot{
    private static final String BOTNAME = "Harry Botter";

    /**
     * This method returns a random move
     */
    public IMove randomMove(IGameState state) {
        Random rand = new Random();
        List<IMove> moves = state.getField().getAvailableMoves();
        if (moves.size() > 0) {
            return moves.get(rand.nextInt(moves.size())); /* get random move from available moves */
        }

        return null;
    }

    /**
     * This method first check if there are any winning moves, if there are then the bot wins.
     * Then checks if it can block the opponent from winning.
     * Else it returns a random move
     * @param
     * @return
     */
    @Override
    public IMove doMove(IGameState state) {

        List<IMove> centerMove = state.getField().getAvailableMoves();
        if (centerMove.size() == 81){
            return centerMove.get(40);
        }

        List<IMove> winMoves = getWinningMoves(state);
        if(!winMoves.isEmpty())
            return winMoves.get(0);

        List<IMove> blockMoves = getBlockingMoves(state);
        if (!blockMoves.isEmpty())
            return blockMoves.get(0);

        return randomMove(state);
    }

    private boolean isWinningMove(IGameState state, IMove move, String player) {
        // Clones the array and all values to a new array, so we don't mess with the game
        String[][] board = Arrays.stream(state.getField().getBoard()).map(String[]::clone).toArray(String[][]::new);

        //Places the player in the game. Sort of a simulation.
        board[move.getX()][move.getY()] = player;

        int localX = move.getX() % 3;
        int localY = move.getY() % 3;
        int startX = move.getX() - (localX);
        int startY = move.getY() - (localY);

        //check col
        for (int i = startY; i < startY + 3; i++) {
            if (!board[move.getX()][i].equals(player))
                break;
            if (i == startY + 3 - 1)
                return true;
        }

        //check row
        for (int i = startX; i < startX + 3; i++) {
            if (!board[i][move.getY()].equals(player))
                break;
            if (i == startX + 3 - 1)
                return true;
        }

        //check diagonal
        if (localX == localY) {
            //we're on a diagonal
            int y = startY;
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][y++].equals(player))
                    break;
                if (i == startX + 3 - 1)
                    return true;
            }
        }

        //check anti diagonal
        if (localX + localY == 3 - 1) {
            int less = 0;
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][(startY + 2)-less++].equals(player))
                    break;
                if (i == startX + 3 - 1)
                    return true;
            }
        }
        return false;
    }

    private Boolean isBlockingMove(IGameState state, IMove move, String player){
        // Clones the array and all values to a new array, so we don't mess with the game
        String[][] board = Arrays.stream(state.getField().getBoard()).map(String[]::clone).toArray(String[][]::new);

        //Places the player in the game. Sort of a simulation.
        board[move.getX()][move.getY()] = player;

        int localX = move.getX() % 3;
        int localY = move.getY() % 3;
        int startX = move.getX() - (localX);
        int startY = move.getY() - (localY);

        //check col
        for (int i = startY; i < startY + 3; i++) {
            if (!board[move.getX()][i].equals(player))
                break;
            if (i == startY + 3 - 1)
                return true;
        }

        //check row
        for (int i = startX; i < startX + 3; i++) {
            if (!board[i][move.getY()].equals(player))
                break;
            if (i == startX + 3 - 1)
                return true;
        }

        //check diagonal
        if (localX == localY) {
            //we're on a diagonal
            int y = startY;
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][y++].equals(player))
                    break;
                if (i == startX + 3 - 1)
                    return true;
            }
        }

        //check anti diagonal
        if (localX + localY == 3 - 1) {
            int less = 0;
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][(startY + 2)-less++].equals(player))
                    break;
                if (i == startX + 3 - 1)
                    return true;
            }
        }
        return false;
    }


    // Compile a list of all available winning moves
    private List<IMove> getWinningMoves(IGameState state){
        String player = "1";
        if(state.getMoveNumber()%2==0)
            player="0";

        List<IMove> avail = state.getField().getAvailableMoves();

        List<IMove> winningMoves = new ArrayList<>();
        for (IMove move:avail) {
            if(isWinningMove(state,move,player))
                winningMoves.add(move);
        }
        return winningMoves;
    }

    // Compile a list of all available blocking moves
    private List<IMove> getBlockingMoves(IGameState state){
        String player = "1";
        if(state.getMoveNumber()%2==1)
            player="0";

        List<IMove> avail = state.getField().getAvailableMoves();

        List<IMove> blockingMoves = new ArrayList<>();
        for (IMove move:avail) {
            if(isBlockingMove(state,move,player))
                blockingMoves.add(move);
        }
        return blockingMoves;
    }

    @Override
    public String getBotName() {
        return BOTNAME;
    }

}
