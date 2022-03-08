package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import dk.easv.bll.move.Move;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//PrioList bot that also checks if you can stop an opponent from making a board winning play;
public class TrashBot implements IBot{

    private static final String BOTNAME="Trash bot";

    protected int[][] preferredMoves = {
            {1, 1}, //Center
            {0, 2}, {2, 0},{0, 0}, {2, 2},  //Corners ordered across
            {0, 1}, {2, 1}, {1, 0}, {1, 2}}; //Outer Middles ordered across


    @Override
    public IMove doMove(IGameState state) {
        List<IMove> winMoves = getWinningMoves(state);
        List<IMove> preventWinMoves = getPreventingMoves(state);
        if (!winMoves.isEmpty())
            return winMoves.get(0);
        else if (!preventWinMoves.isEmpty())
            return preventWinMoves.get(0);
       return getPriorityMove(state);
    }

    private IMove getPriorityMove(IGameState state) {
        //Find macroboard to play in
        for (int[] move : preferredMoves)
        {
            if(state.getField().getMacroboard()[move[0]][move[1]].equals(IField.AVAILABLE_FIELD))
            {
                //find move to play
                for (int[] selectedMove : preferredMoves)
                {
                    int x = move[0]*3 + selectedMove[0];
                    int y = move[1]*3 + selectedMove[1];
                    if(state.getField().getBoard()[x][y].equals(IField.EMPTY_FIELD))
                    {
                        return new Move(x,y);
                    }
                }
            }
        }

        //NOTE: Something failed, just take the first available move I guess!
        return state.getField().getAvailableMoves().get(0);

    }

    private List<IMove> getPreventingMoves(IGameState state) {
        String player = "0";
        if (state.getMoveNumber()%2==0)
            player = "1";
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        List<IMove> preventingMoves = new ArrayList<>();

        for(IMove move: availableMoves)
            if (isWinningMove(state,move,player))
                preventingMoves.add(move);
        return preventingMoves;
    }


    private List<IMove> getWinningMoves(IGameState state) {
        String player = "1";
        if (state.getMoveNumber()%2==0)
            player = "0";

        List<IMove> availableMoves = state.getField().getAvailableMoves();
        List<IMove> winningMoves = new ArrayList<>();

        for(IMove move : availableMoves)
            if (isWinningMove(state,move,player))
                winningMoves.add(move);

        return winningMoves;
    }

    private boolean isWinningMove(IGameState state, IMove move, String player) {
        String[][] board = Arrays.stream(state.getField().getBoard()).map(String[]::clone).toArray(String[][]::new);

        /*for (int i = 0; i < board.length ; i++) {
            for (int j = 0; j < board[i].length; j++) {
                System.out.print(board[i][j]+" ");
            }
            System.out.println("");
        }

         */

        board[move.getX()][move.getY()] = player;

        int startX = move.getX()-(move.getX()%3);
        int startY = move.getY()-(move.getY()%3);

        //horizontal checking
        if(board[startX][move.getY()].equals(player))
            if (board[startX][move.getY()].equals(board[startX+1][move.getY()]) &&
                board[startX+1][move.getY()].equals(board[startX+2][move.getY()]))
                    return true;

        //Vertical checking
        if (board[move.getX()][startY].equals(player))
            if (board[move.getX()][startY].equals(board[move.getX()][startY+1]) &&
                board[move.getX()][startY+1].equals(board[move.getX()][startY+2])){
                System.out.println("matches vertically");
                    return true;
            }
        //Diagonal checking TopLeft-BottomRight
        if(board[startX][startY].equals(player))
            if (board[startX+1][startY+1].equals(board[startX][startY]) &&
                board[startX+2][startY+2].equals(board[startX+1][startY+1])){
                System.out.println("matches diagonally");
                return true;
            }

        if(board[startX+2][startY].equals(player))
            if (board[startX+2][startY].equals(board[startX+1][startY+1]) &&
                    board[startX][startY+2].equals(board[startX+1][startY+1])){
                System.out.println("matches diagonally opposite");
                return true;
            }



        return false;
    }

    @Override
    public String getBotName() {
        return BOTNAME;
    }
}
